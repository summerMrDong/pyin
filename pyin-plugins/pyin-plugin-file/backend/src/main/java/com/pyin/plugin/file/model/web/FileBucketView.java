package com.pyin.plugin.file.model.web;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class FileBucketView {

    private String bucketName;
    private String description;
    private boolean publicRead;
    private List<String> allowedTypes;

}
