package com.pyin.plugin.file.model;

public class PutObjectResult {

    private final String bucketName;
    private final String objectKey;
    private final String storageName;
    private final long fileSize;
    private final String sha256;

    public PutObjectResult(String bucketName, String objectKey, String storageName, long fileSize, String sha256) {
        this.bucketName = bucketName;
        this.objectKey = objectKey;
        this.storageName = storageName;
        this.fileSize = fileSize;
        this.sha256 = sha256;
    }

    public String getBucketName() {
        return bucketName;
    }

    public String getObjectKey() {
        return objectKey;
    }

    public String getStorageName() {
        return storageName;
    }

    public long getFileSize() {
        return fileSize;
    }

    public String getSha256() {
        return sha256;
    }
}
