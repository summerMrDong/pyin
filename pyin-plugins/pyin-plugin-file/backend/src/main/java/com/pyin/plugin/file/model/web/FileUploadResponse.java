package com.pyin.plugin.file.model.web;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FileUploadResponse {

    private String fileId;
    private String bucketName;
    private String objectKey;
    private String originalName;
    private String fileExt;
    private long fileSize;
    private boolean image;
    private String accessLevel;
    private String previewUrl;
    private String downloadUrl;
    private String contentType;
    private String realMimeType;
    private Integer imageWidth;
    private Integer imageHeight;
    private String bizType;
    private String bizId;
    private String status;
    private long downloadCount;
    private String createdAt;

}
