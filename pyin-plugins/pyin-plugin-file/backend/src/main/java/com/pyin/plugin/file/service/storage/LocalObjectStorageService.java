package com.pyin.plugin.file.service.storage;

import com.pyin.plugin.common.code.ErrorCode;
import com.pyin.plugin.file.exception.FilePluginBusinessException;
import com.pyin.plugin.file.model.PutObjectResult;
import com.pyin.plugin.file.support.FilePathGuard;
import com.pyin.plugin.file.support.FileStorageProperties;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class LocalObjectStorageService implements ObjectStorageService {

    private final FileStorageProperties fileStorageProperties;

    public LocalObjectStorageService(FileStorageProperties fileStorageProperties) {
        this.fileStorageProperties = fileStorageProperties;
    }

    @Override
    public PutObjectResult putObject(String bucketName, String objectKey, MultipartFile file) {
        try {
            Path target = FilePathGuard.resolveInside(fileStorageProperties.rootPathAsPath(), bucketName, objectKey);
            Files.createDirectories(target.getParent());
            byte[] bytes = file.getBytes();
            Files.write(target, bytes);
            return new PutObjectResult(
                    bucketName,
                    objectKey,
                    target.getFileName().toString(),
                    bytes.length,
                    sha256(bytes)
            );
        } catch (IOException exception) {
            throw new FilePluginBusinessException(ErrorCode.INVALID_REQUEST, "文件写入失败。");
        }
    }

    @Override
    public InputStream getObject(String bucketName, String objectKey) {
        try {
            Path target = FilePathGuard.resolveInside(fileStorageProperties.rootPathAsPath(), bucketName, objectKey);
            return Files.newInputStream(target);
        } catch (IOException exception) {
            throw new FilePluginBusinessException(ErrorCode.NOT_FOUND, "文件不存在。");
        }
    }

    @Override
    public boolean exists(String bucketName, String objectKey) {
        Path target = FilePathGuard.resolveInside(fileStorageProperties.rootPathAsPath(), bucketName, objectKey);
        return Files.exists(target);
    }

    @Override
    public void deleteObject(String bucketName, String objectKey) {
        Path target = FilePathGuard.resolveInside(fileStorageProperties.rootPathAsPath(), bucketName, objectKey);
        try {
            Files.deleteIfExists(target);
        } catch (IOException exception) {
            throw new FilePluginBusinessException(ErrorCode.INVALID_REQUEST, "文件删除失败。");
        }
    }

    @Override
    public String getStorageType() {
        return "LOCAL_OSS";
    }

    private static String sha256(byte[] bytes) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(bytes));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("Missing SHA-256 algorithm", exception);
        }
    }
}
