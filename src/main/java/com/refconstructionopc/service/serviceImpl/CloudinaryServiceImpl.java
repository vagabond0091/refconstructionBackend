package com.refconstructionopc.service.serviceImpl;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.refconstructionopc.service.CloudinaryService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
@Service
public class CloudinaryServiceImpl implements CloudinaryService {
    private final Cloudinary cloudinary;

    @Value("${cloudinary.folder:uploads}")
    private String baseFolder;

    @Value("${cloudinary.cloud-name}")
    private String cloudName;

    @Value("${uploads.large-threshold-bytes:8000000}") // 8 MB
    private long largeThreshold;
    public CloudinaryServiceImpl(Cloudinary cloudinary) {
        this.cloudinary = cloudinary;
    }
    @Override
    public String uploadAndGetKey(MultipartFile file, String subFolder) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }
        // Delegate to the byte[] overload to keep logic in one place
        return uploadAndGetKey(file.getBytes(), subFolder);
    }

    @Override
    public String uploadAndGetKey(byte[] bytes, String subFolder) throws IOException {
        if (bytes == null || bytes.length == 0) {
            throw new IllegalArgumentException("Bytes are empty");
        }
        String folder = (subFolder == null || subFolder.isBlank())
                ? baseFolder
                : baseFolder + "/" + subFolder;

        Map<?, ?> res = cloudinary.uploader().upload(
                bytes,
                ObjectUtils.asMap(
                        "folder", folder,
                        "resource_type", "image",
                        "use_filename", true,
                        "unique_filename", true,
                        "overwrite", false
                )
        );
        return (String) res.get("public_id");
    }
    @Override
    public String uploadAndGetKeyLarge(InputStream in, String subFolder) throws IOException {
        if (in == null) throw new IllegalArgumentException("InputStream is null");
        Map<?, ?> res = cloudinary.uploader().uploadLarge(
                in,
                ObjectUtils.asMap(
                        "folder", folderPath(subFolder),
                        "resource_type", "image",
                        "chunk_size", 6_000_000, // ~6 MB chunks; tune as needed
                        "use_filename", true,
                        "unique_filename", true,
                        "overwrite", false
                )
        );
        return (String) res.get("public_id");
    }
    @Override
    public String smartUpload(MultipartFile file, String subFolder) throws IOException {
        if (file.getSize() > largeThreshold) {
            try (var in = file.getInputStream()) {
                // chunked; good for big files and flaky networks
                Map<?, ?> res = cloudinary.uploader().uploadLarge(in, ObjectUtils.asMap(
                        "folder", folderPath(subFolder),
                        "resource_type", "image",
                        "chunk_size", 6_000_000
                ));
                return (String) res.get("public_id");
            }
        }
        return uploadAndGetKey(file.getBytes(), subFolder); // your fast path for small files
    }
    @Override
    public boolean deleteByKey(String publicId) throws IOException {
        if (publicId == null || publicId.isBlank()) return false;

        // If you only ever upload images, this is enough:
        Map<?, ?> res = cloudinary.uploader().destroy(
                publicId,
                ObjectUtils.asMap(
                        "invalidate", true,     // purge cached versions on the CDN
                        "resource_type", "image"
                )
        );

        Object result = res.get("result"); // "ok", "not found", "error"
        return "ok".equals(result) || "not found".equals(result);
    }

    private String folderPath(String subFolder) {
        return (subFolder == null || subFolder.isBlank()) ? baseFolder : baseFolder + "/" + subFolder;
    }
}
