package com.pyin.plugin.file.service;

import com.pyin.plugin.common.code.ErrorCode;
import com.pyin.plugin.file.exception.FilePluginBusinessException;
import com.pyin.plugin.file.model.FileInfoRecord;
import com.pyin.plugin.file.model.PutObjectResult;
import com.pyin.plugin.file.model.web.FileUploadResponse;
import com.pyin.plugin.file.service.storage.ObjectStorageService;
import com.pyin.plugin.file.support.FileBucketProperties;
import com.pyin.plugin.file.support.FileNameGenerator;
import com.pyin.plugin.file.support.FilePathGuard;
import com.pyin.plugin.file.support.FileStorageProperties;
import com.pyin.plugin.file.support.FileTypeDetector;
import com.pyin.plugin.file.support.ImageMetadataReader;
import jakarta.annotation.PostConstruct;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
public class FileAdminService {

    private final JdbcTemplate jdbcTemplate;
    private final FileStorageProperties fileStorageProperties;
    private final ObjectStorageService objectStorageService;
    private final FileQueryService fileQueryService;

    public FileAdminService(
            JdbcTemplate jdbcTemplate,
            FileStorageProperties fileStorageProperties,
            ObjectStorageService objectStorageService,
            FileQueryService fileQueryService
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.fileStorageProperties = fileStorageProperties;
        this.objectStorageService = objectStorageService;
        this.fileQueryService = fileQueryService;
    }

    @PostConstruct
    public void initialize() {
        if (!StringUtils.hasText(fileStorageProperties.getDefaultBucket())) {
            throw new IllegalStateException("pyin.center.file-storage.default-bucket is required");
        }
        if (!fileStorageProperties.getBuckets().containsKey(fileStorageProperties.getDefaultBucket())) {
            throw new IllegalStateException("Default bucket must exist in pyin.center.file-storage.buckets");
        }
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS pyin_plugin_file_info (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    file_id VARCHAR(64) NOT NULL,
                    bucket_name VARCHAR(64) NOT NULL,
                    object_key VARCHAR(512) NOT NULL,
                    original_name VARCHAR(255) NOT NULL,
                    storage_name VARCHAR(255) NOT NULL,
                    file_ext VARCHAR(32) NOT NULL,
                    content_type VARCHAR(255),
                    real_mime_type VARCHAR(255) NOT NULL,
                    file_size BIGINT NOT NULL,
                    file_sha256 VARCHAR(128) NOT NULL,
                    storage_type VARCHAR(64) NOT NULL,
                    is_image VARCHAR(8) NOT NULL,
                    image_width INT,
                    image_height INT,
                    biz_type VARCHAR(128),
                    biz_id VARCHAR(128),
                    access_level VARCHAR(32) NOT NULL,
                    download_count BIGINT NOT NULL DEFAULT 0,
                    status VARCHAR(32) NOT NULL,
                    delete_flag VARCHAR(50) NOT NULL DEFAULT 'NOT_DELETE',
                    created_at TIMESTAMP NOT NULL,
                    created_by VARCHAR(128),
                    updated_at TIMESTAMP NOT NULL,
                    updated_by VARCHAR(128),
                    CONSTRAINT uk_pyin_plugin_file_info_file_id UNIQUE (file_id),
                    CONSTRAINT uk_pyin_plugin_file_info_bucket_object_key UNIQUE (bucket_name, object_key)
                )
                """);
        executeIgnoreFailure("CREATE INDEX idx_pyin_plugin_file_info_biz_type_biz_id ON pyin_plugin_file_info (biz_type, biz_id)");
        executeIgnoreFailure("CREATE INDEX idx_pyin_plugin_file_info_status_delete_flag ON pyin_plugin_file_info (status, delete_flag)");
        executeIgnoreFailure("CREATE INDEX idx_pyin_plugin_file_info_created_at ON pyin_plugin_file_info (created_at)");
    }

    public FileUploadResponse upload(MultipartFile file, String bucketName, String bizType, String bizId) {
        if (file == null || file.isEmpty()) {
            throw new FilePluginBusinessException(ErrorCode.INVALID_REQUEST, "上传文件不能为空。");
        }
        if (file.getSize() > fileStorageProperties.getMaxFileSizeBytes()) {
            throw new FilePluginBusinessException(ErrorCode.INVALID_REQUEST, "文件大小超过限制。");
        }

        String resolvedBucket = resolveBucketName(bucketName);
        FileBucketProperties bucketProperties = requireBucket(resolvedBucket);
        String originalName = StringUtils.hasText(file.getOriginalFilename()) ? file.getOriginalFilename().trim() : "unnamed";
        String fileExt = FileTypeDetector.extensionOf(originalName);
        if (!StringUtils.hasText(fileExt)) {
            throw new FilePluginBusinessException(ErrorCode.INVALID_REQUEST, "文件扩展名不能为空。");
        }
        if (!FileTypeDetector.isAllowed(fileExt, bucketProperties.getAllowedTypes())) {
            throw new FilePluginBusinessException(ErrorCode.INVALID_REQUEST, "当前 bucket 不允许上传该类型文件。");
        }

        boolean image = FileTypeDetector.isImageExtension(fileExt);
        String objectKey = FileNameGenerator.buildObjectKey(image, fileExt);
        String fileId = "file_" + UUID.randomUUID().toString().replace("-", "");
        String realMimeType = FileTypeDetector.mimeTypeFor(fileExt, file.getContentType());
        PutObjectResult putObjectResult = objectStorageService.putObject(resolvedBucket, objectKey, file);

        Integer imageWidth = null;
        Integer imageHeight = null;
        if (image) {
            try {
                ImageMetadataReader.ImageSize imageSize = ImageMetadataReader.read(file.getBytes());
                imageWidth = imageSize.width();
                imageHeight = imageSize.height();
            } catch (Exception ignored) {
                imageWidth = null;
                imageHeight = null;
            }
        }

        Timestamp now = Timestamp.from(Instant.now());
        jdbcTemplate.update("""
                        INSERT INTO pyin_plugin_file_info (
                            file_id, bucket_name, object_key, original_name, storage_name, file_ext,
                            content_type, real_mime_type, file_size, file_sha256, storage_type,
                            is_image, image_width, image_height, biz_type, biz_id, access_level,
                            download_count, status, delete_flag, created_at, created_by, updated_at, updated_by
                        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """,
                fileId,
                resolvedBucket,
                objectKey,
                originalName,
                putObjectResult.getStorageName(),
                fileExt,
                file.getContentType(),
                realMimeType,
                putObjectResult.getFileSize(),
                putObjectResult.getSha256(),
                objectStorageService.getStorageType(),
                image ? "YES" : "NO",
                imageWidth,
                imageHeight,
                trimToNull(bizType),
                trimToNull(bizId),
                bucketProperties.isPublicRead() ? "PUBLIC_READ" : "PRIVATE",
                0L,
                "NORMAL",
                "NOT_DELETE",
                now,
                "system",
                now,
                "system"
        );
        FileInfoRecord record = fileQueryService.requireActiveRecord(fileId);
        return fileQueryService.toResponse(record);
    }

    public List<FileUploadResponse> uploadBatch(MultipartFile[] files, String bucketName, String bizType, String bizId) {
        if (files == null || files.length == 0) {
            throw new FilePluginBusinessException(ErrorCode.INVALID_REQUEST, "上传文件不能为空。");
        }
        long totalSize = 0L;
        for (MultipartFile file : files) {
            if (file == null || file.isEmpty()) {
                throw new FilePluginBusinessException(ErrorCode.INVALID_REQUEST, "批量上传中存在空文件。");
            }
            totalSize += file.getSize();
        }
        if (totalSize > fileStorageProperties.getMaxRequestSizeBytes()) {
            throw new FilePluginBusinessException(ErrorCode.INVALID_REQUEST, "本次请求文件总大小超过限制。");
        }
        List<FileUploadResponse> responses = new ArrayList<>();
        for (MultipartFile file : files) {
            responses.add(upload(file, bucketName, bizType, bizId));
        }
        return responses;
    }

    public void deleteFile(String fileId) {
        FileInfoRecord record = fileQueryService.requireActiveRecord(fileId);
        jdbcTemplate.update("""
                UPDATE pyin_plugin_file_info
                SET status = 'DELETED', delete_flag = 'DELETED', updated_at = CURRENT_TIMESTAMP, updated_by = ?
                WHERE file_id = ?
                """, "system", record.getFileId());
    }

    public Map<String, Object> summary() {
        return fileQueryService.summary();
    }

    private String resolveBucketName(String bucketName) {
        String resolved = StringUtils.hasText(bucketName) ? bucketName.trim() : fileStorageProperties.getDefaultBucket();
        FilePathGuard.validateBucketName(resolved);
        requireBucket(resolved);
        return resolved;
    }

    private FileBucketProperties requireBucket(String bucketName) {
        FileBucketProperties bucket = fileStorageProperties.getBuckets().get(bucketName);
        if (bucket == null) {
            throw new FilePluginBusinessException(ErrorCode.INVALID_REQUEST, "bucketName 不存在。");
        }
        return bucket;
    }

    private void executeIgnoreFailure(String sql) {
        try {
            jdbcTemplate.execute(sql);
        } catch (Exception ignored) {
        }
    }

    private static String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }
}
