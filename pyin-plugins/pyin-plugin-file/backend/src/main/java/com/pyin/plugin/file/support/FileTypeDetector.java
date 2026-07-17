package com.pyin.plugin.file.support;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.util.StringUtils;

public final class FileTypeDetector {

    private static final Set<String> IMAGE_TYPES = Set.of("jpg", "jpeg", "png", "gif", "webp", "bmp");

    private FileTypeDetector() {
    }

    public static String extensionOf(String originalName) {
        if (!StringUtils.hasText(originalName) || !originalName.contains(".")) {
            return "";
        }
        String ext = originalName.substring(originalName.lastIndexOf('.') + 1).trim().toLowerCase(Locale.ROOT);
        return ext.replaceAll("[^a-z0-9]", "");
    }

    public static boolean isImageExtension(String ext) {
        return IMAGE_TYPES.contains(normalize(ext));
    }

    public static String mimeTypeFor(String ext, String fallbackContentType) {
        String normalized = normalize(ext);
        return switch (normalized) {
            case "jpg", "jpeg" -> "image/jpeg";
            case "png" -> "image/png";
            case "gif" -> "image/gif";
            case "webp" -> "image/webp";
            case "bmp" -> "image/bmp";
            case "pdf" -> "application/pdf";
            case "doc" -> "application/msword";
            case "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            case "xls" -> "application/vnd.ms-excel";
            case "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            case "zip" -> "application/zip";
            default -> StringUtils.hasText(fallbackContentType) ? fallbackContentType : "application/octet-stream";
        };
    }

    public static boolean isAllowed(String ext, List<String> allowedTypes) {
        String normalized = normalize(ext);
        return allowedTypes.stream().map(FileTypeDetector::normalize).anyMatch(normalized::equals);
    }

    private static String normalize(String ext) {
        return ext == null ? "" : ext.trim().toLowerCase(Locale.ROOT);
    }
}
