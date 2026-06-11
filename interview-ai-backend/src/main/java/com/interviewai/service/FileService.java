package com.interviewai.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

/**
 * 文件存储服务。本地文件系统存储，生产环境可替换为OSS。
 */
@Slf4j
@Service
public class FileService {

    private final Path uploadDir;

    public FileService(@Value("${app.upload-dir:./uploads}") String uploadDir) {
        this.uploadDir = Paths.get(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.uploadDir);
        } catch (IOException e) {
            log.error("创建上传目录失败: {}", this.uploadDir, e);
        }
    }

    /**
     * 保存上传文件到根目录，返回访问路径。
     */
    public String save(MultipartFile file) {
        return save(file, null);
    }

    /**
     * 保存上传文件到指定子目录，返回访问路径。
     * 文件名 = UUID + 原始扩展名，避免冲突和安全问题。
     */
    public String save(MultipartFile file, String subDir) {
        try {
            String originalName = file.getOriginalFilename();
            String ext = "";
            if (originalName != null && originalName.contains(".")) {
                ext = originalName.substring(originalName.lastIndexOf("."));
            }
            String storedName = UUID.randomUUID().toString() + ext;
            Path targetDir = subDir != null ? uploadDir.resolve(subDir) : uploadDir;
            Files.createDirectories(targetDir);
            Path target = targetDir.resolve(storedName);
            file.transferTo(target.toFile());
            log.info("文件保存成功: {}", target);
            String urlPath = subDir != null ? "/uploads/" + subDir + "/" + storedName : "/uploads/" + storedName;
            return urlPath;
        } catch (IOException e) {
            log.error("文件保存失败", e);
            throw new RuntimeException("文件保存失败", e);
        }
    }

    public Path getFilePath(String fileUrl) {
        // fileUrl like "/uploads/resumes/uuid.pdf" or "/uploads/uuid.pdf"
        String relative = fileUrl.startsWith("/uploads/")
                ? fileUrl.substring("/uploads/".length())
                : fileUrl.substring(fileUrl.lastIndexOf("/") + 1);
        return uploadDir.resolve(relative);
    }
}
