package com.pyin.plugin.config.service;

import com.pyin.plugin.common.code.ErrorCode;
import com.pyin.plugin.common.exception.BusinessException;
import com.pyin.plugin.config.web.ConfigItemSaveRequest;
import com.pyin.plugin.config.web.ConfigNamespaceSaveRequest;
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
public class ConfigAdminService {

    private static final RowMapper<Map<String, Object>> NAMESPACE_ROW_MAPPER = (resultSet, rowNum) -> {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", resultSet.getLong("id"));
        row.put("namespaceCode", resultSet.getString("namespace_code"));
        row.put("env", resultSet.getString("env"));
        row.put("displayName", resultSet.getString("display_name"));
        row.put("description", resultSet.getString("description"));
        row.put("itemCount", resultSet.getInt("item_count"));
        row.put("createdAt", asIsoString(resultSet, "created_at"));
        row.put("updatedAt", asIsoString(resultSet, "updated_at"));
        return row;
    };

    private static final RowMapper<Map<String, Object>> ITEM_ROW_MAPPER = (resultSet, rowNum) -> {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", resultSet.getLong("id"));
        row.put("namespaceId", resultSet.getLong("namespace_id"));
        row.put("namespaceCode", resultSet.getString("namespace_code"));
        row.put("env", resultSet.getString("env"));
        row.put("displayName", resultSet.getString("display_name"));
        row.put("itemKey", resultSet.getString("item_key"));
        row.put("itemValue", resultSet.getString("item_value"));
        row.put("valueType", resultSet.getString("value_type"));
        row.put("description", resultSet.getString("description"));
        row.put("createdAt", asIsoString(resultSet, "created_at"));
        row.put("updatedAt", asIsoString(resultSet, "updated_at"));
        return row;
    };

    private final JdbcTemplate jdbcTemplate;

    public ConfigAdminService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostConstruct
    public void initialize() {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS pyin_plugin_config_namespace (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    namespace_code VARCHAR(128) NOT NULL,
                    env VARCHAR(64) NOT NULL,
                    display_name VARCHAR(128) NOT NULL,
                    description VARCHAR(512),
                    created_at TIMESTAMP NOT NULL,
                    updated_at TIMESTAMP NOT NULL,
                    CONSTRAINT uk_pyin_plugin_config_namespace UNIQUE (namespace_code, env)
                )
                """);
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS pyin_plugin_config_item (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    namespace_id BIGINT NOT NULL,
                    item_key VARCHAR(160) NOT NULL,
                    item_value VARCHAR(4000) NOT NULL,
                    value_type VARCHAR(32) NOT NULL,
                    description VARCHAR(512),
                    created_at TIMESTAMP NOT NULL,
                    updated_at TIMESTAMP NOT NULL,
                    CONSTRAINT uk_pyin_plugin_config_item UNIQUE (namespace_id, item_key),
                    CONSTRAINT fk_pyin_plugin_config_item_namespace
                        FOREIGN KEY (namespace_id) REFERENCES pyin_plugin_config_namespace(id)
                )
                """);
        seedDemoData();
    }

    public List<Map<String, Object>> listNamespaces() {
        return jdbcTemplate.query("""
                SELECT ns.id,
                       ns.namespace_code,
                       ns.env,
                       ns.display_name,
                       ns.description,
                       ns.created_at,
                       ns.updated_at,
                       COUNT(item.id) AS item_count
                FROM pyin_plugin_config_namespace ns
                LEFT JOIN pyin_plugin_config_item item ON item.namespace_id = ns.id
                GROUP BY ns.id, ns.namespace_code, ns.env, ns.display_name,
                         ns.description, ns.created_at, ns.updated_at
                ORDER BY ns.namespace_code, ns.env
                """, NAMESPACE_ROW_MAPPER);
    }

    public Map<String, Object> saveNamespace(ConfigNamespaceSaveRequest request) {
        String namespaceCode = requireText(request.getNamespaceCode(), "namespaceCode");
        String env = requireText(request.getEnv(), "env");
        String displayName = requireText(request.getDisplayName(), "displayName");
        String description = trimToNull(request.getDescription());

        Long existingId = jdbcTemplate.query("""
                        SELECT id
                        FROM pyin_plugin_config_namespace
                        WHERE namespace_code = ? AND env = ?
                        """,
                resultSet -> resultSet.next() ? resultSet.getLong("id") : null,
                namespaceCode,
                env
        );
        if (request.getId() == null && existingId != null) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "相同 namespaceCode 和 env 已存在。");
        }
        if (request.getId() != null && existingId != null && !request.getId().equals(existingId)) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "命名空间编码与环境组合已被其他记录占用。");
        }

        Timestamp now = Timestamp.from(Instant.now());
        if (request.getId() == null) {
            jdbcTemplate.update("""
                            INSERT INTO pyin_plugin_config_namespace
                                (namespace_code, env, display_name, description, created_at, updated_at)
                            VALUES (?, ?, ?, ?, ?, ?)
                            """,
                    namespaceCode,
                    env,
                    displayName,
                    description,
                    now,
                    now
            );
        } else {
            requireNamespaceExists(request.getId());
            jdbcTemplate.update("""
                            UPDATE pyin_plugin_config_namespace
                            SET namespace_code = ?, env = ?, display_name = ?, description = ?, updated_at = ?
                            WHERE id = ?
                            """,
                    namespaceCode,
                    env,
                    displayName,
                    description,
                    now,
                    request.getId()
            );
        }
        return Map.of("saved", true);
    }

    public void deleteNamespace(Long id) {
        requireNamespaceExists(id);
        Integer itemCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM pyin_plugin_config_item WHERE namespace_id = ?",
                Integer.class,
                id
        );
        if (itemCount != null && itemCount > 0) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "该命名空间下仍有配置项，无法删除。");
        }
        jdbcTemplate.update("DELETE FROM pyin_plugin_config_namespace WHERE id = ?", id);
    }

    public List<Map<String, Object>> listItems(Long namespaceId, String keyword) {
        String normalizedKeyword = trimToNull(keyword);
        boolean hasKeyword = StringUtils.hasText(normalizedKeyword);
        if (namespaceId == null) {
            if (hasKeyword) {
                return jdbcTemplate.query("""
                        SELECT item.id,
                               item.namespace_id,
                               ns.namespace_code,
                               ns.env,
                               ns.display_name,
                               item.item_key,
                               item.item_value,
                               item.value_type,
                               item.description,
                               item.created_at,
                               item.updated_at
                        FROM pyin_plugin_config_item item
                        JOIN pyin_plugin_config_namespace ns ON ns.id = item.namespace_id
                        WHERE item.item_key LIKE ? OR item.item_value LIKE ?
                        ORDER BY ns.namespace_code, ns.env, item.item_key
                        """, ITEM_ROW_MAPPER, like(normalizedKeyword), like(normalizedKeyword));
            }
            return jdbcTemplate.query("""
                    SELECT item.id,
                           item.namespace_id,
                           ns.namespace_code,
                           ns.env,
                           ns.display_name,
                           item.item_key,
                           item.item_value,
                           item.value_type,
                           item.description,
                           item.created_at,
                           item.updated_at
                    FROM pyin_plugin_config_item item
                    JOIN pyin_plugin_config_namespace ns ON ns.id = item.namespace_id
                    ORDER BY ns.namespace_code, ns.env, item.item_key
                    """, ITEM_ROW_MAPPER);
        }

        requireNamespaceExists(namespaceId);
        if (hasKeyword) {
            return jdbcTemplate.query("""
                    SELECT item.id,
                           item.namespace_id,
                           ns.namespace_code,
                           ns.env,
                           ns.display_name,
                           item.item_key,
                           item.item_value,
                           item.value_type,
                           item.description,
                           item.created_at,
                           item.updated_at
                    FROM pyin_plugin_config_item item
                    JOIN pyin_plugin_config_namespace ns ON ns.id = item.namespace_id
                    WHERE item.namespace_id = ?
                      AND (item.item_key LIKE ? OR item.item_value LIKE ?)
                    ORDER BY item.item_key
                    """, ITEM_ROW_MAPPER, namespaceId, like(normalizedKeyword), like(normalizedKeyword));
        }
        return jdbcTemplate.query("""
                SELECT item.id,
                       item.namespace_id,
                       ns.namespace_code,
                       ns.env,
                       ns.display_name,
                       item.item_key,
                       item.item_value,
                       item.value_type,
                       item.description,
                       item.created_at,
                       item.updated_at
                FROM pyin_plugin_config_item item
                JOIN pyin_plugin_config_namespace ns ON ns.id = item.namespace_id
                WHERE item.namespace_id = ?
                ORDER BY item.item_key
                """, ITEM_ROW_MAPPER, namespaceId);
    }

    public Map<String, Object> getItem(Long id) {
        List<Map<String, Object>> rows = jdbcTemplate.query("""
                SELECT item.id,
                       item.namespace_id,
                       ns.namespace_code,
                       ns.env,
                       ns.display_name,
                       item.item_key,
                       item.item_value,
                       item.value_type,
                       item.description,
                       item.created_at,
                       item.updated_at
                FROM pyin_plugin_config_item item
                JOIN pyin_plugin_config_namespace ns ON ns.id = item.namespace_id
                WHERE item.id = ?
                """, ITEM_ROW_MAPPER, id);
        if (rows.isEmpty()) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "配置项不存在。");
        }
        return rows.get(0);
    }

    public Map<String, Object> saveItem(ConfigItemSaveRequest request) {
        Long namespaceId = request.getNamespaceId();
        if (namespaceId == null) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "namespaceId 不能为空。");
        }
        requireNamespaceExists(namespaceId);

        String itemKey = requireText(request.getItemKey(), "itemKey");
        String itemValue = requireText(request.getItemValue(), "itemValue");
        String valueType = defaultText(request.getValueType(), "STRING");
        String description = trimToNull(request.getDescription());

        Long existingId = jdbcTemplate.query("""
                        SELECT id
                        FROM pyin_plugin_config_item
                        WHERE namespace_id = ? AND item_key = ?
                        """,
                resultSet -> resultSet.next() ? resultSet.getLong("id") : null,
                namespaceId,
                itemKey
        );
        if (request.getId() == null && existingId != null) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "同一命名空间下的配置键已存在。");
        }
        if (request.getId() != null && existingId != null && !request.getId().equals(existingId)) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "配置键已被其他记录占用。");
        }

        Timestamp now = Timestamp.from(Instant.now());
        if (request.getId() == null) {
            jdbcTemplate.update("""
                            INSERT INTO pyin_plugin_config_item
                                (namespace_id, item_key, item_value, value_type, description, created_at, updated_at)
                            VALUES (?, ?, ?, ?, ?, ?, ?)
                            """,
                    namespaceId,
                    itemKey,
                    itemValue,
                    valueType,
                    description,
                    now,
                    now
            );
        } else {
            requireItemExists(request.getId());
            jdbcTemplate.update("""
                            UPDATE pyin_plugin_config_item
                            SET namespace_id = ?, item_key = ?, item_value = ?, value_type = ?, description = ?, updated_at = ?
                            WHERE id = ?
                            """,
                    namespaceId,
                    itemKey,
                    itemValue,
                    valueType,
                    description,
                    now,
                    request.getId()
            );
        }
        return Map.of("saved", true);
    }

    public void deleteItem(Long id) {
        requireItemExists(id);
        jdbcTemplate.update("DELETE FROM pyin_plugin_config_item WHERE id = ?", id);
    }

    private void seedDemoData() {
        Integer namespaceCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM pyin_plugin_config_namespace",
                Integer.class
        );
        if (namespaceCount != null && namespaceCount > 0) {
            return;
        }

        Timestamp now = Timestamp.from(Instant.now());
        jdbcTemplate.update("""
                        INSERT INTO pyin_plugin_config_namespace
                            (namespace_code, env, display_name, description, created_at, updated_at)
                        VALUES (?, ?, ?, ?, ?, ?)
                        """,
                "order-service",
                "prod",
                "订单服务生产环境",
                "管理订单服务在生产环境下的关键参数。",
                now,
                now
        );
        jdbcTemplate.update("""
                        INSERT INTO pyin_plugin_config_namespace
                            (namespace_code, env, display_name, description, created_at, updated_at)
                        VALUES (?, ?, ?, ?, ?, ?)
                        """,
                "order-service",
                "test",
                "订单服务测试环境",
                "用于测试环境联调的命名空间。",
                now,
                now
        );

        Long prodNamespaceId = jdbcTemplate.queryForObject(
                "SELECT id FROM pyin_plugin_config_namespace WHERE namespace_code = ? AND env = ?",
                Long.class,
                "order-service",
                "prod"
        );
        Long testNamespaceId = jdbcTemplate.queryForObject(
                "SELECT id FROM pyin_plugin_config_namespace WHERE namespace_code = ? AND env = ?",
                Long.class,
                "order-service",
                "test"
        );

        jdbcTemplate.update("""
                        INSERT INTO pyin_plugin_config_item
                            (namespace_id, item_key, item_value, value_type, description, created_at, updated_at)
                        VALUES (?, ?, ?, ?, ?, ?, ?)
                        """,
                prodNamespaceId,
                "order.timeout.seconds",
                "45",
                "INTEGER",
                "订单超时时间。",
                now,
                now
        );
        jdbcTemplate.update("""
                        INSERT INTO pyin_plugin_config_item
                            (namespace_id, item_key, item_value, value_type, description, created_at, updated_at)
                        VALUES (?, ?, ?, ?, ?, ?, ?)
                        """,
                prodNamespaceId,
                "order.notify.enabled",
                "true",
                "BOOLEAN",
                "是否开启订单通知。",
                now,
                now
        );
        jdbcTemplate.update("""
                        INSERT INTO pyin_plugin_config_item
                            (namespace_id, item_key, item_value, value_type, description, created_at, updated_at)
                        VALUES (?, ?, ?, ?, ?, ?, ?)
                        """,
                testNamespaceId,
                "order.mock.mode",
                "sandbox",
                "STRING",
                "测试环境模拟模式。",
                now,
                now
        );
    }

    private void requireNamespaceExists(Long id) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM pyin_plugin_config_namespace WHERE id = ?",
                Integer.class,
                id
        );
        if (count == null || count == 0) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "命名空间不存在。");
        }
    }

    private void requireItemExists(Long id) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM pyin_plugin_config_item WHERE id = ?",
                Integer.class,
                id
        );
        if (count == null || count == 0) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "配置项不存在。");
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

    private static String like(String keyword) {
        return "%" + keyword + "%";
    }

    private static String asIsoString(ResultSet resultSet, String column) throws SQLException {
        Timestamp timestamp = resultSet.getTimestamp(column);
        return timestamp == null ? null : timestamp.toInstant().toString();
    }
}
