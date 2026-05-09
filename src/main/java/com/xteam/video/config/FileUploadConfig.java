package com.xteam.video.config;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class FileUploadConfig {

    private static final Logger log = LoggerFactory.getLogger(FileUploadConfig.class);

    @Value("${file.upload.path:/tmp/video-upload/}")
    private String uploadPath;

    @PostConstruct
    public void init() {
        try {
            Path path = Paths.get(uploadPath);
            if (!Files.exists(path)) {
                Files.createDirectories(path);
                log.info("Created upload directory: {}", uploadPath);
            }
        } catch (Exception e) {
            log.error("Failed to create upload directory: {}", e.getMessage(), e);
        }
    }
}