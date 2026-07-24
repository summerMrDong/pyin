package com.pyin.plugin.file.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
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

}
