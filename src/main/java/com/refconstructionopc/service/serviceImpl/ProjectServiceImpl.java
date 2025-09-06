package com.refconstructionopc.service.serviceImpl;
import com.refconstructionopc.dto.ProjectDTO;
import com.refconstructionopc.dto.ProjectImageDTO;
import com.refconstructionopc.model.Project;
import com.refconstructionopc.model.ProjectImage;
import com.refconstructionopc.repository.ProjectRepository;
import com.refconstructionopc.service.CloudinaryService;
import com.refconstructionopc.service.ProjectService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.data.domain.*;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProjectServiceImpl implements ProjectService {
    private static final Logger log = LoggerFactory.getLogger(ProjectServiceImpl.class);

    private final ProjectRepository projectRepository;

    private final CloudinaryService cloudinaryService;

    @Qualifier("imageUploadExecutor")
    private final AsyncTaskExecutor imageUploadExecutor;

    @Value("${uploads.parallel-timeout-seconds:120}")
    private long parallelTimeoutSeconds;

    public ProjectServiceImpl(ProjectRepository projectRepository, CloudinaryService cloudinaryService, AsyncTaskExecutor imageUploadExecutor) {
        this.projectRepository = projectRepository;
        this.cloudinaryService = cloudinaryService;
        this.imageUploadExecutor = imageUploadExecutor;
    }

    @Transactional
    @Override
    public ProjectDTO create(String title, String description, String serviceType,
                             MultipartFile thumbnail, List<MultipartFile> images) throws IOException {

        final long t0 = System.currentTimeMillis();
        final String uuid = UUID.randomUUID().toString();

        // Track uploaded keys for fail-fast cleanup
        final Queue<String> uploadedKeys = new ConcurrentLinkedQueue<>();

        // Instrumentation counters
        final AtomicInteger inflight = new AtomicInteger(0);
        final AtomicInteger maxInflight = new AtomicInteger(0);
        final AtomicInteger seq = new AtomicInteger(0);

        try {
            // 1) Schedule gallery uploads in parallel (overlaps with thumbnail)
            List<CompletableFuture<String>> futures = new ArrayList<>();
            if (images != null && !images.isEmpty()) {
                for (MultipartFile imageFile : images) {
                    if (imageFile == null || imageFile.isEmpty()) continue;

                    final int id = seq.incrementAndGet();
                    final long sizeKb = imageFile.getSize() / 1024;

                    futures.add(CompletableFuture.supplyAsync(() -> {
                        long start = System.currentTimeMillis();
                        int now = inflight.incrementAndGet();
                        maxInflight.updateAndGet(m -> Math.max(m, now));
                        log.info("UPLOAD-START #{} size={}KB inflight={}", id, sizeKb, now);
                        try {
                            String key = cloudinaryService.smartUpload(imageFile, uuid);
                            uploadedKeys.add(key);
                            return key;
                        } catch (IOException e) {
                            throw new CompletionException(e);
                        } finally {
                            long took = System.currentTimeMillis() - start;
                            int after = inflight.decrementAndGet();
                            log.info("UPLOAD-END   #{} took={}ms inflight={}", id, took, after);
                        }
                    }, imageUploadExecutor));
                }
            }

            // Snapshot the executor state right after scheduling
            if (imageUploadExecutor instanceof ThreadPoolTaskExecutor tp) {
                log.info("EXEC state: poolSize={}, active={}, queueSize={}",
                        tp.getPoolSize(), tp.getActiveCount(), tp.getThreadPoolExecutor().getQueue().size());
            }

            // 2) Upload thumbnail on request thread (overlaps with gallery)
            long tThumb = System.currentTimeMillis();
            String thumbKey = cloudinaryService.smartUpload(thumbnail, uuid);
            uploadedKeys.add(thumbKey);
            log.info("Thumbnail {} KB -> {} ms (key={})",
                    thumbnail.getSize() / 1024, System.currentTimeMillis() - tThumb, thumbKey);

            // 3) Build entity (persist once at the end)
            Project data = new Project();
            data.setUniqueId(uuid);
            data.setTitle(title);
            data.setDescription(description);
            data.setServiceType(serviceType);
            data.setThumbnailImage(thumbKey);
            data.setImages(new ArrayList<>());

            // 4) Await all gallery uploads with timeout; cleanup on error
            if (!futures.isEmpty()) {
                CompletableFuture<Void> all = CompletableFuture.allOf(
                        futures.toArray(new CompletableFuture[0])
                );
                try {
                    all.orTimeout(parallelTimeoutSeconds, TimeUnit.SECONDS).join();
                } catch (Throwable t) {
                    futures.forEach(f -> f.cancel(true));
                    // best-effort cleanup (thumbnail + any completed gallery keys)
                    for (String k : uploadedKeys) {
                        try { cloudinaryService.deleteByKey(k); } catch (Exception ignore) {}
                    }
                    throw new RuntimeException("One or more uploads failed or timed out after "
                            + parallelTimeoutSeconds + "s", t);
                }
            }

            // 5) Attach images and save (cascade persists children)
            for (CompletableFuture<String> f : futures) {
                String key = f.join(); // safe after allOf completed
                ProjectImage img = new ProjectImage();
                img.setProject(data);
                img.setImageUrl(key);
                data.getImages().add(img);
            }

            // Summary instrumentation
            log.info("UPLOAD-SUMMARY maxInflight={}", maxInflight.get());

            data = projectRepository.save(data);

            long ms = System.currentTimeMillis() - t0;
            log.info("Created project {} ({} images) in {} ms",
                    data.getId(), (images == null ? 0 : images.size()), ms);

            return convertProjectEntityToDTO(data);

        } catch (Exception e) {
            log.error("Create project failed: {}", e.getMessage());
            // Best-effort cleanup of anything that made it to Cloudinary
            for (String k : uploadedKeys) {
                try { cloudinaryService.deleteByKey(k); } catch (Exception ignore) {}
            }
            throw e;
        }
    }

    @Override
    @Transactional
    public Page<ProjectDTO> findAllProject(String search,int page, int size) {
        int p = Math.max(page, 0);
        int s = (size < 1) ? 10 : size; // any default ≥1; no upper cap
        Pageable pageable = PageRequest.of(p, s); // no Sort here

        String q = (search == null || search.isBlank()) ? null : search.trim();

        // DB returns all matches already ordered by createdAt DESC
        List<Project> all = projectRepository.getAllProjectWithSearch(q);
        int total = all.size();

        int start = (int) pageable.getOffset();
        if (start >= total) {
            return new PageImpl<>(List.of(), pageable, total);
        }
        int end = Math.min(start + pageable.getPageSize(), total);

        // map only the slice
        List<ProjectDTO> results = all.subList(start, end).stream()
                .map(this::convertProjectEntityToDTO)
                .toList();

        return new PageImpl<>(results, pageable, total);
    }

    private ProjectDTO convertProjectEntityToDTO(Project data) {
        ProjectDTO dto = new ProjectDTO();
        dto.setId(data.getId());
        dto.setUniqueId(data.getUniqueId());
        dto.setTitle(data.getTitle());
        dto.setDescription(data.getDescription());
        dto.setServiceType(data.getServiceType());
        dto.setThumbnailImage(data.getThumbnailImage());

        List<ProjectImageDTO> imageDTOs = new ArrayList<>();
        if (data.getImages() != null) {
            for (ProjectImage pi : data.getImages()) {
                ProjectImageDTO img = new ProjectImageDTO();
                img.setId(pi.getId());
                img.setImageUrl(pi.getImageUrl()); // Cloudinary public_id
                imageDTOs.add(img);
            }
        }
        dto.setImages(imageDTOs);
        dto.setCreatedAt(data.getCreatedAt());
        dto.setUpdatedAt(data.getUpdatedAt());
        return dto;
    }
}
