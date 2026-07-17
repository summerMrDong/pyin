package com.pyin.plugin.file.support;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "pyin.center.file-storage")
public class FileStorageProperties {

    private String type = "LOCAL_OSS";
    private String rootPath = "pyin-distribution-parent/runtime/pyin-config-center-runtime/data/plugin-data/file-storage";
    private String defaultBucket = "business";
    private long maxFileSizeBytes = 20L * 1024 * 1024;
    private long maxRequestSizeBytes = 100L * 1024 * 1024;
    private Map<String, FileBucketProperties> buckets = new LinkedHashMap<>();

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getRootPath() {
        return rootPath;
    }

    public void setRootPath(String rootPath) {
        this.rootPath = rootPath;
    }

    public String getDefaultBucket() {
        return defaultBucket;
    }

    public void setDefaultBucket(String defaultBucket) {
        this.defaultBucket = defaultBucket;
    }

    public long getMaxFileSizeBytes() {
        return maxFileSizeBytes;
    }

    public void setMaxFileSizeBytes(long maxFileSizeBytes) {
        this.maxFileSizeBytes = maxFileSizeBytes;
    }

    public long getMaxRequestSizeBytes() {
        return maxRequestSizeBytes;
    }

    public void setMaxRequestSizeBytes(long maxRequestSizeBytes) {
        this.maxRequestSizeBytes = maxRequestSizeBytes;
    }

    public Map<String, FileBucketProperties> getBuckets() {
        return buckets;
    }

    public void setBuckets(Map<String, FileBucketProperties> buckets) {
        this.buckets = buckets;
    }

    public Path rootPathAsPath() {
        return Paths.get(rootPath).toAbsolutePath().normalize();
    }
}
