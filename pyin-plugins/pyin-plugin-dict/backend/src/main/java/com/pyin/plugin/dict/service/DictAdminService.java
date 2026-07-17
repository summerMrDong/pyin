package com.pyin.plugin.dict.service;

import com.pyin.plugin.common.code.ErrorCode;
import com.pyin.plugin.common.exception.BusinessException;
import com.pyin.plugin.dict.web.DictItemSaveRequest;
import com.pyin.plugin.dict.web.DictTypeSaveRequest;
import jakarta.annotation.PostConstruct;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class DictAdminService {

    private static final RowMapper<Map<String, Object>> TYPE_ROW_MAPPER = (resultSet, rowNum) -> {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", resultSet.getLong("id"));
        row.put("typeCode", resultSet.getString("type_code"));
        row.put("typeName", resultSet.getString("type_name"));
        row.put("status", resultSet.getString("status"));
        row.put("description", resultSet.getString("description"));
        row.put("itemCount", resultSet.getInt("item_count"));
        row.put("createdAt", asIsoString(resultSet, "created_at"));
        row.put("updatedAt", asIsoString(resultSet, "updated_at"));
        return row;
    };

    private static final RowMapper<Map<String, Object>> ITEM_ROW_MAPPER = (resultSet, rowNum) -> {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", resultSet.getLong("id"));
        row.put("typeId", resultSet.getLong("type_id"));
        row.put("typeCode", resultSet.getString("type_code"));
        row.put("typeName", resultSet.getString("type_name"));
        row.put("itemValue", resultSet.getString("item_value"));
        row.put("itemLabel", resultSet.getString("item_label"));
        row.put("itemSort", resultSet.getInt("item_sort"));
        row.put("itemStatus", resultSet.getString("item_status"));
        row.put("description", resultSet.getString("description"));
        row.put("createdAt", asIsoString(resultSet, "created_at"));
        row.put("updatedAt", asIsoString(resultSet, "updated_at"));
        return row;
    };

    private final JdbcTemplate jdbcTemplate;

    public DictAdminService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostConstruct
    public void initialize() {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS pyin_plugin_dict_type (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    type_code VARCHAR(128) NOT NULL,
                    type_name VARCHAR(128) NOT NULL,
                    status VARCHAR(32) NOT NULL,
                    description VARCHAR(512),
                    created_at TIMESTAMP NOT NULL,
                    updated_at TIMESTAMP NOT NULL,
                    CONSTRAINT uk_pyin_plugin_dict_type UNIQUE (type_code)
                )
                """);
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS pyin_plugin_dict_item (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    type_id BIGINT NOT NULL,
                    item_value VARCHAR(128) NOT NULL,
                    item_label VARCHAR(128) NOT NULL,
                    item_sort INT NOT NULL,
                    item_status VARCHAR(32) NOT NULL,
                    description VARCHAR(512),
                    created_at TIMESTAMP NOT NULL,
                    updated_at TIMESTAMP NOT NULL,
                    CONSTRAINT uk_pyin_plugin_dict_item UNIQUE (type_id, item_value),
                    CONSTRAINT fk_pyin_plugin_dict_item_type
                        FOREIGN KEY (type_id) REFERENCES pyin_plugin_dict_type(id)
                )
                """);
        seedDemoData();
    }

    public List<Map<String, Object>> listTypes() {
        return jdbcTemplate.query("""
                SELECT dict_type.id,
                       dict_type.type_code,
                       dict_type.type_name,
                       dict_type.status,
                       dict_type.description,
                       dict_type.created_at,
                       dict_type.updated_at,
                       COUNT(item.id) AS item_count
                FROM pyin_plugin_dict_type dict_type
                LEFT JOIN pyin_plugin_dict_item item ON item.type_id = dict_type.id
                GROUP BY dict_type.id, dict_type.type_code, dict_type.type_name, dict_type.status,
                         dict_type.description, dict_type.created_at, dict_type.updated_at
                ORDER BY dict_type.type_code
                """, TYPE_ROW_MAPPER);
    }

    public Map<String, Object> saveType(DictTypeSaveRequest request) {
        String typeCode = requireText(request.getTypeCode(), "typeCode");
        String typeName = requireText(request.getTypeName(), "typeName");
        String status = defaultText(request.getStatus(), "ENABLED");
        String description = trimToNull(request.getDescription());

        Long existingId = jdbcTemplate.query("""
                        SELECT id
                        FROM pyin_plugin_dict_type
                        WHERE type_code = ?
                        """,
                resultSet -> resultSet.next() ? resultSet.getLong("id") : null,
                typeCode
        );
        if (request.getId() == null && existingId != null) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "字典类型编码已存在。");
        }
        if (request.getId() != null && existingId != null && !request.getId().equals(existingId)) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "字典类型编码已被其他记录占用。");
        }

        Timestamp now = Timestamp.from(Instant.now());
        if (request.getId() == null) {
            jdbcTemplate.update("""
                            INSERT INTO pyin_plugin_dict_type
                                (type_code, type_name, status, description, created_at, updated_at)
                            VALUES (?, ?, ?, ?, ?, ?)
                            """,
                    typeCode,
                    typeName,
                    status,
                    description,
                    now,
                    now
            );
        } else {
            requireTypeExists(request.getId());
            jdbcTemplate.update("""
                            UPDATE pyin_plugin_dict_type
                            SET type_code = ?, type_name = ?, status = ?, description = ?, updated_at = ?
                            WHERE id = ?
                            """,
                    typeCode,
                    typeName,
                    status,
                    description,
                    now,
                    request.getId()
            );
        }
        return Map.of("saved", true);
    }

    public void deleteType(Long id) {
        requireTypeExists(id);
        Integer itemCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM pyin_plugin_dict_item WHERE type_id = ?",
                Integer.class,
                id
        );
        if (itemCount != null && itemCount > 0) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "该字典类型下仍有字典项，无法删除。");
        }
        jdbcTemplate.update("DELETE FROM pyin_plugin_dict_type WHERE id = ?", id);
    }

    public List<Map<String, Object>> listItems(Long typeId) {
        if (typeId == null) {
            return jdbcTemplate.query("""
                    SELECT item.id,
                           item.type_id,
                           dict_type.type_code,
                           dict_type.type_name,
                           item.item_value,
                           item.item_label,
                           item.item_sort,
                           item.item_status,
                           item.description,
                           item.created_at,
                           item.updated_at
                    FROM pyin_plugin_dict_item item
                    JOIN pyin_plugin_dict_type dict_type ON dict_type.id = item.type_id
                    ORDER BY dict_type.type_code, item.item_sort, item.id
                    """, ITEM_ROW_MAPPER);
        }
        requireTypeExists(typeId);
        return jdbcTemplate.query("""
                SELECT item.id,
                       item.type_id,
                       dict_type.type_code,
                       dict_type.type_name,
                       item.item_value,
                       item.item_label,
                       item.item_sort,
                       item.item_status,
                       item.description,
                       item.created_at,
                       item.updated_at
                FROM pyin_plugin_dict_item item
                JOIN pyin_plugin_dict_type dict_type ON dict_type.id = item.type_id
                WHERE item.type_id = ?
                ORDER BY item.item_sort, item.id
                """, ITEM_ROW_MAPPER, typeId);
    }

    public Map<String, Object> getItem(Long id) {
        List<Map<String, Object>> rows = jdbcTemplate.query("""
                SELECT item.id,
                       item.type_id,
                       dict_type.type_code,
                       dict_type.type_name,
                       item.item_value,
                       item.item_label,
                       item.item_sort,
                       item.item_status,
                       item.description,
                       item.created_at,
                       item.updated_at
                FROM pyin_plugin_dict_item item
                JOIN pyin_plugin_dict_type dict_type ON dict_type.id = item.type_id
                WHERE item.id = ?
                """, ITEM_ROW_MAPPER, id);
        if (rows.isEmpty()) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "字典项不存在。");
        }
        return rows.get(0);
    }

    public Map<String, Object> saveItem(DictItemSaveRequest request) {
        Long typeId = request.getTypeId();
        if (typeId == null) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "typeId 不能为空。");
        }
        requireTypeExists(typeId);

        String itemValue = requireText(request.getItemValue(), "itemValue");
        String itemLabel = requireText(request.getItemLabel(), "itemLabel");
        Integer itemSort = request.getItemSort() == null ? 100 : request.getItemSort();
        String itemStatus = defaultText(request.getItemStatus(), "ENABLED");
        String description = trimToNull(request.getDescription());

        Long existingId = jdbcTemplate.query("""
                        SELECT id
                        FROM pyin_plugin_dict_item
                        WHERE type_id = ? AND item_value = ?
                        """,
                resultSet -> resultSet.next() ? resultSet.getLong("id") : null,
                typeId,
                itemValue
        );
        if (request.getId() == null && existingId != null) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "同一字典类型下的字典值已存在。");
        }
        if (request.getId() != null && existingId != null && !request.getId().equals(existingId)) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "字典值已被其他记录占用。");
        }

        Timestamp now = Timestamp.from(Instant.now());
        if (request.getId() == null) {
            jdbcTemplate.update("""
                            INSERT INTO pyin_plugin_dict_item
                                (type_id, item_value, item_label, item_sort, item_status, description, created_at, updated_at)
                            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                            """,
                    typeId,
                    itemValue,
                    itemLabel,
                    itemSort,
                    itemStatus,
                    description,
                    now,
                    now
            );
        } else {
            requireItemExists(request.getId());
            jdbcTemplate.update("""
                            UPDATE pyin_plugin_dict_item
                            SET type_id = ?, item_value = ?, item_label = ?, item_sort = ?, item_status = ?, description = ?, updated_at = ?
                            WHERE id = ?
                            """,
                    typeId,
                    itemValue,
                    itemLabel,
                    itemSort,
                    itemStatus,
                    description,
                    now,
                    request.getId()
            );
        }
        return Map.of("saved", true);
    }

    public void deleteItem(Long id) {
        requireItemExists(id);
        jdbcTemplate.update("DELETE FROM pyin_plugin_dict_item WHERE id = ?", id);
    }

    private void seedDemoData() {
        Integer typeCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM pyin_plugin_dict_type",
                Integer.class
        );
        if (typeCount != null && typeCount > 0) {
            return;
        }

        Timestamp now = Timestamp.from(Instant.now());
        jdbcTemplate.update("""
                        INSERT INTO pyin_plugin_dict_type
                            (type_code, type_name, status, description, created_at, updated_at)
                        VALUES (?, ?, ?, ?, ?, ?)
                        """,
                "gender",
                "性别",
                "ENABLED",
                "系统默认性别字典。",
                now,
                now
        );
        jdbcTemplate.update("""
                        INSERT INTO pyin_plugin_dict_type
                            (type_code, type_name, status, description, created_at, updated_at)
                        VALUES (?, ?, ?, ?, ?, ?)
                        """,
                "order_status",
                "订单状态",
                "ENABLED",
                "订单流程状态字典。",
                now,
                now
        );

        Long genderTypeId = jdbcTemplate.queryForObject(
                "SELECT id FROM pyin_plugin_dict_type WHERE type_code = ?",
                Long.class,
                "gender"
        );
        Long orderStatusTypeId = jdbcTemplate.queryForObject(
                "SELECT id FROM pyin_plugin_dict_type WHERE type_code = ?",
                Long.class,
                "order_status"
        );

        insertSeedItem(genderTypeId, "1", "男", 10, now);
        insertSeedItem(genderTypeId, "2", "女", 20, now);
        insertSeedItem(orderStatusTypeId, "CREATED", "已创建", 10, now);
        insertSeedItem(orderStatusTypeId, "PAID", "已支付", 20, now);
    }

    private void insertSeedItem(Long typeId, String value, String label, int sort, Timestamp now) {
        jdbcTemplate.update("""
                        INSERT INTO pyin_plugin_dict_item
                            (type_id, item_value, item_label, item_sort, item_status, description, created_at, updated_at)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                        """,
                typeId,
                value,
                label,
                sort,
                "ENABLED",
                "系统初始化数据",
                now,
                now
        );
    }

    private void requireTypeExists(Long id) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM pyin_plugin_dict_type WHERE id = ?",
                Integer.class,
                id
        );
        if (count == null || count == 0) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "字典类型不存在。");
        }
    }

    private void requireItemExists(Long id) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM pyin_plugin_dict_item WHERE id = ?",
                Integer.class,
                id
        );
        if (count == null || count == 0) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "字典项不存在。");
        }
    }

    private static String requireText(String value, String fieldName) {
        String normalized = trimToNull(value);
        if (!StringUtils.hasText(normalized)) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, fieldName + " 不能为空。");
        }
        return normalized;
    }

    private static String defaultText(String value, String defaultValue) {
        String normalized = trimToNull(value);
        return StringUtils.hasText(normalized) ? normalized : defaultValue;
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static String asIsoString(ResultSet resultSet, String column) throws SQLException {
        Timestamp timestamp = resultSet.getTimestamp(column);
        return timestamp == null ? null : timestamp.toInstant().toString();
    }
}
