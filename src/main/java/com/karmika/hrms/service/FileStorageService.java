package com.karmika.hrms.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.util.UUID;

@Service
public class FileStorageService {

    @Value("${app.upload-dir:uploads}")
    private String uploadDir;

    public String saveFile(MultipartFile file, String subDir) throws IOException {
        if (file == null || file.isEmpty()) {
            return null;
        }

        String ext = "";
        String original = file.getOriginalFilename();
        if (original != null && original.contains(".")) {
            ext = original.substring(original.lastIndexOf("."));
        }

        String filename = UUID.randomUUID() + ext;
        Path dir = Paths.get(uploadDir, subDir);
        Files.createDirectories(dir);
        Path target = dir.resolve(filename);
        Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);

        // URL-friendly path served by WebConfig static handler
        return "/uploads/" + subDir + "/" + filename;
    }
}
