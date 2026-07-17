package com.pyin.plugin.file.service.storage;

import com.pyin.plugin.file.model.PutObjectResult;
import java.io.InputStream;
import org.springframework.web.multipart.MultipartFile;

public interface ObjectStorageService {

    PutObjectResult putObject(String bucketName, String objectKey, MultipartFile file);

    InputStream getObject(String bucketName, String objectKey);

    boolean exists(String bucketName, String objectKey);

    void deleteObject(String bucketName, String objectKey);

    String getStorageType();
}
