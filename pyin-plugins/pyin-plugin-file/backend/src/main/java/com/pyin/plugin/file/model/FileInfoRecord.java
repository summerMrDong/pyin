package com.pyin.plugin.file.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FileInfoRecord {

    private Long id;
    private String fileId;
    private String bucketName;
    private String objectKey;
    private String originalName;
    private String storageName;
    private String fileExt;
    private String contentType;
    private String realMimeType;
    private long fileSize;
    private String fileSha256;
    private String storageType;
    private String isImage;
    private Integer imageWidth;
    private Integer imageHeight;
    private String bizType;
    private String bizId;
    private String accessLevel;
    private long downloadCount;
    private String status;
    private String deleteFlag;
    private String createdAt;
    private String createdBy;
    private String updatedAt;
    private String updatedBy;

}
