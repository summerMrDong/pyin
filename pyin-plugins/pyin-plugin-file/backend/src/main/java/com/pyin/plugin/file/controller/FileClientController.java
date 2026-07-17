package com.pyin.plugin.file.controller;

import com.pyin.plugin.common.api.Result;
import com.pyin.plugin.common.exception.BusinessException;
import com.pyin.plugin.sdk.annotation.OpenMapping;
import com.pyin.plugin.file.model.FileBinaryResource;
import com.pyin.plugin.file.model.web.FileUploadResponse;
import com.pyin.plugin.file.service.FileAdminService;
import com.pyin.plugin.file.service.FileQueryService;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

@OpenMapping
public class FileClientController {

    private final FileAdminService fileAdminService;
    private final FileQueryService fileQueryService;

    public FileClientController(FileAdminService fileAdminService, FileQueryService fileQueryService) {
        this.fileAdminService = fileAdminService;
        this.fileQueryService = fileQueryService;
    }

    @PostMapping("/files/upload")
    public Result<FileUploadResponse> upload(
            @RequestPart("file") MultipartFile file,
            @RequestParam(required = false) String bucketName,
            @RequestParam(required = false) String bizType,
            @RequestParam(required = false) String bizId
    ) {
        try {
            return Result.ok(fileAdminService.upload(file, bucketName, bizType, bizId));
        } catch (BusinessException exception) {
            return Result.fail(exception.getCode(), exception.getMessage());
        }
    }

    @PostMapping("/files/upload-batch")
    public Result<List<FileUploadResponse>> uploadBatch(
            @RequestPart("files") MultipartFile[] files,
            @RequestParam(required = false) String bucketName,
            @RequestParam(required = false) String bizType,
            @RequestParam(required = false) String bizId
    ) {
        try {
            return Result.ok(fileAdminService.uploadBatch(files, bucketName, bizType, bizId));
        } catch (BusinessException exception) {
            return Result.fail(exception.getCode(), exception.getMessage());
        }
    }

    @GetMapping("/files/{fileId}")
    public Result<FileUploadResponse> detail(@PathVariable String fileId) {
        try {
            return Result.ok(fileQueryService.getFileDetail(fileId));
        } catch (BusinessException exception) {
            return Result.fail(exception.getCode(), exception.getMessage());
        }
    }

    @GetMapping("/files")
    public Result<List<FileUploadResponse>> list(
            @RequestParam(required = false) String bizType,
            @RequestParam(required = false) String bizId
    ) {
        return Result.ok(fileQueryService.listFiles(bizType, bizId));
    }

    @GetMapping("/files/{fileId}/preview")
    public ResponseEntity<InputStreamResource> preview(@PathVariable String fileId) {
        try {
            FileBinaryResource resource = fileQueryService.openPreview(fileId);
            return binaryResponse(resource.getInputStream(), resource.getRecord().getRealMimeType(), null);
        } catch (BusinessException exception) {
            throw responseStatusException(exception);
        }
    }

    @GetMapping("/files/{fileId}/download")
    public ResponseEntity<InputStreamResource> download(@PathVariable String fileId) {
        try {
            FileBinaryResource resource = fileQueryService.openDownload(fileId);
            return binaryResponse(resource.getInputStream(), resource.getRecord().getRealMimeType(), resource.getRecord().getOriginalName());
        } catch (BusinessException exception) {
            throw responseStatusException(exception);
        }
    }

    private ResponseEntity<InputStreamResource> binaryResponse(InputStream stream, String contentType, String downloadFileName) {
        HttpHeaders headers = new HttpHeaders();
        if (downloadFileName != null) {
            String encoded = URLEncoder.encode(downloadFileName, StandardCharsets.UTF_8).replace("+", "%20");
            headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encoded);
        }
        MediaType mediaType = MediaType.parseMediaType(contentType == null ? MediaType.APPLICATION_OCTET_STREAM_VALUE : contentType);
        return ResponseEntity.ok()
                .headers(headers)
                .contentType(mediaType)
                .body(new InputStreamResource(stream));
    }

    private ResponseStatusException responseStatusException(BusinessException exception) {
        HttpStatus status = switch (exception.getCode()) {
            case "PYIN-403" -> HttpStatus.FORBIDDEN;
            case "PYIN-404" -> HttpStatus.NOT_FOUND;
            case "PYIN-400" -> HttpStatus.BAD_REQUEST;
            default -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
        return new ResponseStatusException(status, exception.getMessage(), exception);
    }
}
