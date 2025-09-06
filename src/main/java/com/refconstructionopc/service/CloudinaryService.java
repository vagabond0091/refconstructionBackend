package com.refconstructionopc.service;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;

public interface CloudinaryService {
    String uploadAndGetKey(MultipartFile file, String subFolder) throws IOException;
    String uploadAndGetKey(byte[] bytes, String subFolder) throws IOException;
    String smartUpload(MultipartFile file, String subFolder) throws IOException;
    String uploadAndGetKeyLarge(InputStream in, String subFolder) throws IOException;
    boolean deleteByKey(String publicId) throws IOException;
}
