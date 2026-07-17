package com.pyin.plugin.file.support;

import com.pyin.plugin.common.code.ErrorCode;
import com.pyin.plugin.file.exception.FilePluginBusinessException;
import java.nio.file.Path;
import org.springframework.util.StringUtils;

public final class FilePathGuard {

    private FilePathGuard() {
    }

    public static void validateBucketName(String bucketName) {
        if (!StringUtils.hasText(bucketName)) {
            throw new FilePluginBusinessException(ErrorCode.INVALID_REQUEST, "bucketName 不能为空。");
        }
        if (bucketName.contains("/") || bucketName.contains("\\") || bucketName.contains("..")) {
            throw new FilePluginBusinessException(ErrorCode.INVALID_REQUEST, "bucketName 非法。");
        }
    }

    public static Path resolveInside(Path rootPath, String bucketName, String objectKey) {
        validateBucketName(bucketName);
        if (!StringUtils.hasText(objectKey) || objectKey.contains("..") || objectKey.startsWith("/") || objectKey.startsWith("\\")) {
            throw new FilePluginBusinessException(ErrorCode.INVALID_REQUEST, "objectKey 非法。");
        }
        Path bucketRoot = rootPath.resolve(bucketName).normalize();
        Path resolved = bucketRoot.resolve(objectKey).normalize();
        if (!resolved.startsWith(bucketRoot)) {
            throw new FilePluginBusinessException(ErrorCode.FORBIDDEN, "检测到非法路径访问。");
        }
        return resolved;
    }
}
