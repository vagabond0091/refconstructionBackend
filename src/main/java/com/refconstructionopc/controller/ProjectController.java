package com.refconstructionopc.controller;

import com.refconstructionopc.dto.ProjectDTO;
import com.refconstructionopc.response.ApiResponse;
import com.refconstructionopc.service.ProjectService;
import com.refconstructionopc.service.serviceImpl.ProjectServiceImpl;
import com.refconstructionopc.validators.ProjectDataValidators;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/projects")
public class ProjectController {
    private static final Logger log = LoggerFactory.getLogger(ProjectController.class);
    @Autowired
    private final ProjectService projectService;

    public ProjectController(ProjectService projectService) {
        this.projectService = projectService;
    }

    @PostMapping(value = "/create", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<ProjectDTO>> create(
            @RequestParam String title,
            @RequestParam(required = false) String description,
            @RequestParam String serviceType,
            @RequestPart("thumbnail") MultipartFile thumbnail,
            @RequestPart(name = "images", required = false) List<MultipartFile> images
    ) {
        ApiResponse<ProjectDTO> resp = new ApiResponse<>();
        Map<String, String> errors = ProjectDataValidators.validate(
                title, description, serviceType, thumbnail, images
        );

        if (!errors.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Validation failed", (ProjectDTO) errors));
        }
       try{
           log.info("Initializing project controller and triggering the create method in projectService");
           ProjectDTO projectDTO = projectService.create(title,description,serviceType,thumbnail,images);
           resp.setMessage("Project Created Successfully.");
           resp.setStatus(HttpStatus.OK.value());
           resp.setData(projectDTO);
           log.info("Creating project is done.");
           return ResponseEntity.ok(resp);
       } catch (Exception e) {
           resp.setMessage(e.getMessage());
           resp.setStatus(HttpStatus.BAD_REQUEST.value());
           resp.setData(null);
           return ResponseEntity.badRequest().body(resp);
       }
    }

    @GetMapping("/getAll")
    public ResponseEntity<ApiResponse<Page<ProjectDTO>>> getAll(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        ApiResponse<ProjectDTO> resp = new ApiResponse<>();
        try{
            Page<ProjectDTO> results = projectService.findAllProject(search,page, size);
            ApiResponse<Page<ProjectDTO>> response = new ApiResponse<>();
            response.setStatus(HttpStatus.OK.value());
            response.setMessage("Fetched project data successfully.");
            response.setData(results);
            return ResponseEntity.ok(response);
        }
        catch (Exception ex){
            log.error("List projects failed", ex);
            ApiResponse<Page<ProjectDTO>> err = new ApiResponse<>();
            err.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
            err.setMessage(ex.getMessage() != null ? ex.getMessage() : "Unexpected server error");
            err.setData(null); // or Page.empty()
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(err);
        }

    }

}
