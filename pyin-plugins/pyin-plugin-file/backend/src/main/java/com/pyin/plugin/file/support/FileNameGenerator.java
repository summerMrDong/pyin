package com.pyin.plugin.file.support;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

public final class FileNameGenerator {

    private static final DateTimeFormatter DATE_PART = DateTimeFormatter.ofPattern("yyyy/MM/dd");

    private FileNameGenerator() {
    }

    public static String buildObjectKey(boolean image, String fileExt) {
        String storageName = UUID.randomUUID().toString().replace("-", "") + "." + fileExt;
        String folder = image ? "image" : "file";
        String datePath = LocalDate.now(ZoneId.systemDefault()).format(DATE_PART);
        return datePath + "/" + folder + "/" + storageName;
    }

    public static String storageNameFromObjectKey(String objectKey) {
        int index = objectKey.lastIndexOf('/');
        return index >= 0 ? objectKey.substring(index + 1) : objectKey;
    }
}
