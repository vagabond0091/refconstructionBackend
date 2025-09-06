package com.refconstructionopc.validators;

import org.springframework.web.multipart.MultipartFile;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public  class ProjectDataValidators {
   public static Map<String, String> validate(
            String title,
            String description,
            String serviceType,
            MultipartFile thumbnail,
            List<MultipartFile> images
    ) {
        Map<String, String> errors = new LinkedHashMap<>();

        if (isBlank(title))       errors.put("title", "title is required");
        if (isBlank(description)) errors.put("description", "description is required");
        if (isBlank(serviceType)) errors.put("serviceType", "serviceType is required");

        if (thumbnail == null || thumbnail.isEmpty()) {
            errors.put("thumbnail", "thumbnail file is required");
        }

        if (images == null || images.isEmpty()) {
            errors.put("images", "images are required and must not be empty");
        } else {
            for (int i = 0; i < images.size(); i++) {
                MultipartFile f = images.get(i);
                if (f == null || f.isEmpty()) {
                    errors.put("images[" + i + "]", "image must not be empty");
                }
            }
        }

        return errors;
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}
