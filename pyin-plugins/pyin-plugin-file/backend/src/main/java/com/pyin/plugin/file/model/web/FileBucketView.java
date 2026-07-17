package com.pyin.plugin.file.model.web;

import java.util.List;

public class FileBucketView {

    private String bucketName;
    private String description;
    private boolean publicRead;
    private List<String> allowedTypes;

    public String getBucketName() {
        return bucketName;
    }

    public void setBucketName(String bucketName) {
        this.bucketName = bucketName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isPublicRead() {
        return publicRead;
    }

    public void setPublicRead(boolean publicRead) {
        this.publicRead = publicRead;
    }

    public List<String> getAllowedTypes() {
        return allowedTypes;
    }

    public void setAllowedTypes(List<String> allowedTypes) {
        this.allowedTypes = allowedTypes;
    }
}
