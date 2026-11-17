package com.yuzhijie.career.trace.module.resume.service;

import com.yuzhijie.career.trace.common.exception.BizException;
import com.yuzhijie.career.trace.common.exception.ErrorCodes;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Set;
import java.util.UUID;

/**
 * 简历本地文件存储：storage/resumes/{userId}/{yyyyMMdd}/{uuid}.{ext}
 */
@Service
public class ResumeStorageService {

    private static final Set<String> ALLOWED_EXT = Set.of("pdf", "docx", "txt", "md");

    private final Path baseDir;
    private final long maxFileSize;

    public ResumeStorageService(@Value("${career-trace.resume.storage-dir:storage/resumes}") String storageDir,
                                @Value("${career-trace.resume.max-file-size:10485760}") long maxFileSize) {
        this.baseDir = Paths.get(storageDir).toAbsolutePath().normalize();
        this.maxFileSize = maxFileSize <= 0 ? 10 * 1024 * 1024 : maxFileSize;
    }

    public String extension(String filename) {
        if (filename == null || !filename.contains(".")) {
            throw BizException.badRequest(ErrorCodes.RESUME_FILE_INVALID,
                    "仅支持 pdf/docx/txt/md 文件");
        }
        String ext = filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
        if (!ALLOWED_EXT.contains(ext)) {
            throw BizException.badRequest(ErrorCodes.RESUME_FILE_INVALID,
                    "仅支持 pdf/docx/txt/md 文件");
        }
        return ext;
    }

    public void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw BizException.badRequest(ErrorCodes.RESUME_FILE_INVALID, "简历文件不能为空");
        }
        if (file.getSize() > maxFileSize) {
            throw BizException.badRequest(ErrorCodes.RESUME_FILE_INVALID, "简历文件不能超过 10MB");
        }
        extension(file.getOriginalFilename());
    }

    public StoredFile store(MultipartFile file, Long userId) {
        validate(file);
        String ext = extension(file.getOriginalFilename());
        String date = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        String relativeDir = userId + "/" + date;
        Path dir = baseDir.resolve(relativeDir).normalize();
        if (!dir.startsWith(baseDir)) {
            throw BizException.badRequest(ErrorCodes.RESUME_FILE_INVALID, "非法文件路径");
        }
        try {
            Files.createDirectories(dir);
            String filename = UUID.randomUUID() + "." + ext;
            Path target = dir.resolve(filename).normalize();
            if (!target.startsWith(baseDir)) {
                throw BizException.badRequest(ErrorCodes.RESUME_FILE_INVALID, "非法文件路径");
            }
            try (var inputStream = file.getInputStream()) {
                Files.copy(inputStream, target, StandardCopyOption.REPLACE_EXISTING);
            }
            String fileKey = relativeDir + "/" + filename;
            return new StoredFile(fileKey, target);
        } catch (IOException e) {
            throw BizException.internal(ErrorCodes.RESUME_PARSE_FAILED, "简历文件保存失败");
        }
    }

    public Path resolve(String fileKey) {
        if (fileKey == null || fileKey.isBlank()) {
            return null;
        }
        Path path = baseDir.resolve(fileKey).normalize();
        return path.startsWith(baseDir) ? path : null;
    }

    public void delete(String fileKey) {
        Path path = resolve(fileKey);
        if (path == null) {
            return;
        }
        try {
            Files.deleteIfExists(path);
        } catch (IOException e) {
            // 文件清理失败不阻塞业务删除
        }
    }

    public record StoredFile(String fileKey, Path path) {
    }
}
