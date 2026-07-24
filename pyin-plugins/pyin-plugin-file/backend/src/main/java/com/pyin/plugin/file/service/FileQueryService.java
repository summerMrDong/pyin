package com.pyin.plugin.file.service;

import com.pyin.plugin.common.code.ErrorCode;
import com.pyin.plugin.file.exception.FilePluginBusinessException;
import com.pyin.plugin.file.model.FileBinaryResource;
import com.pyin.plugin.file.model.FileInfoRecord;
import com.pyin.plugin.file.model.web.FileBucketView;
import com.pyin.plugin.file.model.web.FileUploadResponse;
import com.pyin.plugin.file.service.storage.ObjectStorageService;
import com.pyin.plugin.file.support.FileBucketProperties;
import com.pyin.plugin.file.support.FileStorageProperties;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class FileQueryService {

    private static final RowMapper<FileInfoRecord> ROW_MAPPER = (resultSet, rowNum) -> mapRecord(resultSet);

    private final JdbcTemplate jdbcTemplate;
    private final FileStorageProperties fileStorageProperties;
    private final ObjectStorageService objectStorageService;

    public FileQueryService(
            JdbcTemplate jdbcTemplate,
            FileStorageProperties fileStorageProperties,
            ObjectStorageService objectStorageService
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.fileStorageProperties = fileStorageProperties;
        this.objectStorageService = objectStorageService;
    }

    public FileInfoRecord requireActiveRecord(String fileId) {
        List<FileInfoRecord> rows = jdbcTemplate.query("""
                SELECT id, file_id, bucket_name, object_key, original_name, storage_name, file_ext,
                       content_type, real_mime_type, file_size, file_sha256, storage_type,
                       is_image, image_width, image_height, biz_type, biz_id, access_level,
                       download_count, status, delete_flag, created_at, created_by, updated_at, updated_by
                FROM pyin_plugin_file_info
                WHERE file_id = ? AND delete_flag = 'NOT_DELETE'
                """, ROW_MAPPER, fileId);
        if (rows.isEmpty()) {
            throw new FilePluginBusinessException(ErrorCode.NOT_FOUND, "文件不存在。");
        }
        return rows.get(0);
    }

    public List<FileUploadResponse> listFiles(String bizType, String bizId) {
        StringBuilder sql = new StringBuilder("""
                SELECT id, file_id, bucket_name, object_key, original_name, storage_name, file_ext,
                       content_type, real_mime_type, file_size, file_sha256, storage_type,
                       is_image, image_width, image_height, biz_type, biz_id, access_level,
                       download_count, status, delete_flag, created_at, created_by, updated_at, updated_by
                FROM pyin_plugin_file_info
                WHERE delete_flag = 'NOT_DELETE'
                """);
        List<Object> args = new ArrayList<>();
        if (StringUtils.hasText(bizType)) {
            sql.append(" AND biz_type = ?");
            args.add(bizType.trim());
        }
        if (StringUtils.hasText(bizId)) {
            sql.append(" AND biz_id = ?");
            args.add(bizId.trim());
        }
        sql.append(" ORDER BY created_at DESC, id DESC");
        return jdbcTemplate.query(sql.toString(), ROW_MAPPER, args.toArray()).stream()
                .map(this::toResponse)
                .toList();
    }

    public FileUploadResponse getFileDetail(String fileId) {
        return toResponse(requireActiveRecord(fileId));
    }

    public FileBinaryResource openPreview(String fileId) {
        FileInfoRecord record = requireActiveRecord(fileId);
        if (!"YES".equals(record.getIsImage())) {
            throw new FilePluginBusinessException(ErrorCode.INVALID_REQUEST, "该文件不支持图片预览。");
        }
        if (!objectStorageService.exists(record.getBucketName(), record.getObjectKey())) {
            throw new FilePluginBusinessException(ErrorCode.NOT_FOUND, "文件不存在。");
        }
        return new FileBinaryResource(record, objectStorageService.getObject(record.getBucketName(), record.getObjectKey()));
    }

    public FileBinaryResource openDownload(String fileId) {
        FileInfoRecord record = requireActiveRecord(fileId);
        if (!objectStorageService.exists(record.getBucketName(), record.getObjectKey())) {
            throw new FilePluginBusinessException(ErrorCode.NOT_FOUND, "文件不存在。");
        }
        jdbcTemplate.update("""
                UPDATE pyin_plugin_file_info
                SET download_count = download_count + 1, updated_at = CURRENT_TIMESTAMP
                WHERE file_id = ?
                """, fileId);
        record.setDownloadCount(record.getDownloadCount() + 1);
        return new FileBinaryResource(record, objectStorageService.getObject(record.getBucketName(), record.getObjectKey()));
    }

    public List<FileBucketView> listBuckets() {
        List<FileBucketView> views = new ArrayList<>();
        for (Map.Entry<String, FileBucketProperties> entry : fileStorageProperties.getBuckets().entrySet()) {
            FileBucketView view = new FileBucketView();
            view.setBucketName(entry.getKey());
            view.setDescription(entry.getValue().getDescription());
            view.setPublicRead(entry.getValue().isPublicRead());
            view.setAllowedTypes(List.copyOf(entry.getValue().getAllowedTypes()));
            views.add(view);
        }
        return views;
    }

    public Map<String, Object> summary() {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM pyin_plugin_file_info WHERE delete_flag = 'NOT_DELETE'",
                Integer.class
        );
        Long totalSize = jdbcTemplate.queryForObject(
                "SELECT COALESCE(SUM(file_size), 0) FROM pyin_plugin_file_info WHERE delete_flag = 'NOT_DELETE'",
                Long.class
        );
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("fileCount", count == null ? 0 : count);
        data.put("totalSize", totalSize == null ? 0L : totalSize);
        data.put("bucketCount", fileStorageProperties.getBuckets().size());
        return data;
    }

    public FileUploadResponse toResponse(FileInfoRecord record) {
        FileUploadResponse response = new FileUploadResponse();
        response.setFileId(record.getFileId());
        response.setBucketName(record.getBucketName());
        response.setObjectKey(record.getObjectKey());
        response.setOriginalName(record.getOriginalName());
        response.setFileExt(record.getFileExt());
        response.setFileSize(record.getFileSize());
        response.setImage("YES".equals(record.getIsImage()));
        response.setAccessLevel(record.getAccessLevel());
        response.setPreviewUrl("/plugins/file/admin/files/" + record.getFileId() + "/preview");
        response.setDownloadUrl("/plugins/file/admin/files/" + record.getFileId() + "/download");
        response.setContentType(record.getContentType());
        response.setRealMimeType(record.getRealMimeType());
        response.setImageWidth(record.getImageWidth());
        response.setImageHeight(record.getImageHeight());
        response.setBizType(record.getBizType());
        response.setBizId(record.getBizId());
        response.setStatus(record.getStatus());
        response.setDownloadCount(record.getDownloadCount());
        response.setCreatedAt(record.getCreatedAt());
        return response;
    }

    private static FileInfoRecord mapRecord(ResultSet resultSet) throws SQLException {
        FileInfoRecord record = new FileInfoRecord();
        record.setId(resultSet.getLong("id"));
        record.setFileId(resultSet.getString("file_id"));
        record.setBucketName(resultSet.getString("bucket_name"));
        record.setObjectKey(resultSet.getString("object_key"));
        record.setOriginalName(resultSet.getString("original_name"));
        record.setStorageName(resultSet.getString("storage_name"));
        record.setFileExt(resultSet.getString("file_ext"));
        record.setContentType(resultSet.getString("content_type"));
        record.setRealMimeType(resultSet.getString("real_mime_type"));
        record.setFileSize(resultSet.getLong("file_size"));
        record.setFileSha256(resultSet.getString("file_sha256"));
        record.setStorageType(resultSet.getString("storage_type"));
        record.setIsImage(resultSet.getString("is_image"));
        record.setImageWidth((Integer) resultSet.getObject("image_width"));
        record.setImageHeight((Integer) resultSet.getObject("image_height"));
        record.setBizType(resultSet.getString("biz_type"));
        record.setBizId(resultSet.getString("biz_id"));
        record.setAccessLevel(resultSet.getString("access_level"));
        record.setDownloadCount(resultSet.getLong("download_count"));
        record.setStatus(resultSet.getString("status"));
        record.setDeleteFlag(resultSet.getString("delete_flag"));
        record.setCreatedAt(asIsoString(resultSet, "created_at"));
        record.setCreatedBy(resultSet.getString("created_by"));
        record.setUpdatedAt(asIsoString(resultSet, "updated_at"));
        record.setUpdatedBy(resultSet.getString("updated_by"));
        return record;
    }

    private static String asIsoString(ResultSet resultSet, String column) throws SQLException {
        Timestamp timestamp = resultSet.getTimestamp(column);
        return timestamp == null ? null : timestamp.toInstant().toString();
    }
}
