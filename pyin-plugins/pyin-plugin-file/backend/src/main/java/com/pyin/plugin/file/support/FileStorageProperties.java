package com.pyin.plugin.file.support;

import lombok.Getter;
import lombok.Setter;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "pyin.center.file-storage")
@Getter
@Setter
public class FileStorageProperties {

    private String type = "LOCAL_OSS";
    private String rootPath = "pyin-distribution-parent/runtime/pyin-config-center-runtime/data/plugin-data/file-storage";
    private String defaultBucket = "business";
    private long maxFileSizeBytes = 20L * 1024 * 1024;
    private long maxRequestSizeBytes = 100L * 1024 * 1024;
    private Map<String, FileBucketProperties> buckets = new LinkedHashMap<>();

    public Path rootPathAsPath() {
        return Paths.get(rootPath).toAbsolutePath().normalize();
    }
}
