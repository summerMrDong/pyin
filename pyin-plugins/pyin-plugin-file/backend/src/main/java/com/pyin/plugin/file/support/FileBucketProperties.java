package com.pyin.plugin.file.support;

import java.util.ArrayList;
import java.util.List;

public class FileBucketProperties {

    private String description;
    private boolean publicRead;
    private List<String> allowedTypes = new ArrayList<>();

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
