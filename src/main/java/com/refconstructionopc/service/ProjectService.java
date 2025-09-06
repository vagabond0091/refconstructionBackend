package com.refconstructionopc.service;


import com.refconstructionopc.dto.ProjectDTO;
import org.springframework.data.domain.Page;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

public interface ProjectService {
    ProjectDTO create(String title, String description, String serviceType, MultipartFile thumbnail, List<MultipartFile> images) throws IOException;
    Page<ProjectDTO> findAllProject(String search,int page, int size);
}