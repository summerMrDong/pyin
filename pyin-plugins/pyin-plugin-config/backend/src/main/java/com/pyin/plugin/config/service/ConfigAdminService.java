package com.pyin.plugin.config.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pyin.plugin.common.code.ErrorCode;
import com.pyin.plugin.common.exception.BusinessException;
import com.pyin.plugin.config.model.ConfigDirectoryMode;
import com.pyin.plugin.config.model.ConfigValueType;
import com.pyin.plugin.config.web.ConfigDirectoryMoveRequest;
import com.pyin.plugin.config.web.ConfigDirectorySaveRequest;
import com.pyin.plugin.config.web.ConfigDirectoryTreeNode;
import com.pyin.plugin.config.web.ConfigItemSaveRequest;
import com.pyin.plugin.config.web.ConfigNamespaceSaveRequest;
import jakarta.annotation.PostConstruct;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class ConfigAdminService {

    private static final Pattern ITEM_KEY_PATTERN = Pattern.compile(
            "^[a-z0-9][A-Za-z0-9_-]{0,63}(?::[a-z0-9][A-Za-z0-9_-]{0,63})*$"
    );
    private static final Pattern DIRECTORY_NAME_PATTERN = Pattern.compile("^[A-Za-z0-9][A-Za-z0-9._-]{0,63}$");
    private static final Pattern INTEGER_PATTERN = Pattern.compile("^-?(0|[1-9]\\d*)$");

    private static final RowMapper<Map<String, Object>> NAMESPACE_ROW_MAPPER = (resultSet, rowNum) -> {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", resultSet.getLong("id"));
        row.put("namespaceCode", resultSet.getString("namespace_code"));
        row.put("env", resultSet.getString("env"));
        row.put("displayName", resultSet.getString("display_name"));
        row.put("description", resultSet.getString("description"));
        row.put("directoryMode", resultSet.getString("directory_mode"));
        row.put("itemCount", resultSet.getInt("item_count"));
        row.put("createdAt", asIsoString(resultSet, "created_at"));
        row.put("updatedAt", asIsoString(resultSet, "updated_at"));
        return row;
    };

    private static final RowMapper<Map<String, Object>> ITEM_ROW_MAPPER = (resultSet, rowNum) -> {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", resultSet.getLong("id"));
        row.put("namespaceId", resultSet.getLong("namespace_id"));
        row.put("directoryId", resultSet.getObject("directory_id", Long.class));
        row.put("namespaceCode", resultSet.getString("namespace_code"));
        row.put("env", resultSet.getString("env"));
        row.put("displayName", resultSet.getString("display_name"));
        row.put("itemKey", resultSet.getString("item_key"));
        row.put("itemValue", resultSet.getString("item_value"));
        row.put("defaultValue", resultSet.getString("default_value"));
        row.put("valueType", resultSet.getString("value_type"));
        row.put("status", resultSet.getString("status"));
        row.put("description", resultSet.getString("description"));
        row.put("createdAt", asIsoString(resultSet, "created_at"));
        row.put("updatedAt", asIsoString(resultSet, "updated_at"));
        return row;
    };

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public ConfigAdminService(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
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
                    directory_mode VARCHAR(32) NOT NULL DEFAULT 'KEY_PROJECTION',
                    created_at TIMESTAMP NOT NULL,
                    updated_at TIMESTAMP NOT NULL,
                    CONSTRAINT uk_pyin_plugin_config_namespace UNIQUE (namespace_code, env)
                )
                """);
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS pyin_plugin_config_item (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    namespace_id BIGINT NOT NULL,
                    directory_id BIGINT,
                    item_key VARCHAR(160) NOT NULL,
                    item_value VARCHAR(4000),
                    default_value VARCHAR(4000),
                    value_type VARCHAR(32) NOT NULL,
                    status VARCHAR(16) NOT NULL DEFAULT 'ENABLED',
                    description VARCHAR(512),
                    created_at TIMESTAMP NOT NULL,
                    updated_at TIMESTAMP NOT NULL,
                    CONSTRAINT uk_pyin_plugin_config_item UNIQUE (namespace_id, item_key),
                    CONSTRAINT fk_pyin_plugin_config_item_namespace
                        FOREIGN KEY (namespace_id) REFERENCES pyin_plugin_config_namespace(id)
                )
                """);
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS pyin_plugin_config_directory (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    namespace_id BIGINT NOT NULL,
                    parent_id BIGINT,
                    name VARCHAR(128) NOT NULL,
                    description VARCHAR(512),
                    sort_order INT NOT NULL DEFAULT 0,
                    created_at TIMESTAMP NOT NULL,
                    updated_at TIMESTAMP NOT NULL,
                    CONSTRAINT fk_pyin_plugin_config_directory_namespace
                        FOREIGN KEY (namespace_id) REFERENCES pyin_plugin_config_namespace(id)
                )
                """);
        ensureColumn("pyin_plugin_config_namespace", "directory_mode", "VARCHAR(32) NOT NULL DEFAULT 'KEY_PROJECTION'");
        ensureColumn("pyin_plugin_config_item", "directory_id", "BIGINT");
        ensureColumn("pyin_plugin_config_item", "default_value", "VARCHAR(4000)");
        ensureColumn("pyin_plugin_config_item", "status", "VARCHAR(16) NOT NULL DEFAULT 'ENABLED'");
        allowNullItemValue();
        jdbcTemplate.update("UPDATE pyin_plugin_config_item SET status = ? WHERE status IS NULL", "ENABLED");
        jdbcTemplate.update("UPDATE pyin_plugin_config_item SET status = ? WHERE status = ?", "ENABLED", "DRAFT");
        jdbcTemplate.update("UPDATE pyin_plugin_config_namespace SET directory_mode = ? WHERE directory_mode IS NULL", ConfigDirectoryMode.KEY_PROJECTION.name());
        ensureIndex("idx_pyin_config_directory_parent", "pyin_plugin_config_directory", "namespace_id, parent_id, sort_order");
        ensureIndex("idx_pyin_config_item_directory", "pyin_plugin_config_item", "directory_id");
        seedDemoData();
    }

    public List<Map<String, Object>> listNamespaces() {
        return jdbcTemplate.query("""
                SELECT ns.id, ns.namespace_code, ns.env, ns.display_name, ns.description, ns.directory_mode,
                       ns.created_at, ns.updated_at, COUNT(item.id) AS item_count
                FROM pyin_plugin_config_namespace ns
                LEFT JOIN pyin_plugin_config_item item ON item.namespace_id = ns.id
                GROUP BY ns.id, ns.namespace_code, ns.env, ns.display_name, ns.description, ns.directory_mode,
                         ns.created_at, ns.updated_at
                ORDER BY ns.namespace_code, ns.env
                """, NAMESPACE_ROW_MAPPER);
    }

    @Transactional
    public Map<String, Object> saveNamespace(ConfigNamespaceSaveRequest request) {
        String namespaceCode = requireText(request.getNamespaceCode(), "namespaceCode", 128);
        String env = requireText(request.getEnv(), "env", 64);
        String displayName = requireText(request.getDisplayName(), "displayName", 128);
        String description = optionalText(request.getDescription(), 512, "description");
        ConfigDirectoryMode directoryMode = ConfigDirectoryMode.from(request.getDirectoryMode());
        Long existingId = jdbcTemplate.query("SELECT id FROM pyin_plugin_config_namespace WHERE namespace_code = ? AND env = ?",
                resultSet -> resultSet.next() ? resultSet.getLong("id") : null, namespaceCode, env);
        if (request.getId() == null && existingId != null) {
            throw invalid("相同 namespaceCode 和 env 已存在。");
        }
        if (request.getId() != null && existingId != null && !request.getId().equals(existingId)) {
            throw invalid("命名空间编码与环境组合已被其他记录占用。");
        }
        Timestamp now = Timestamp.from(Instant.now());
        if (request.getId() == null) {
            jdbcTemplate.update("""
                    INSERT INTO pyin_plugin_config_namespace
                        (namespace_code, env, display_name, description, directory_mode, created_at, updated_at)
                    VALUES (?, ?, ?, ?, ?, ?, ?)
                    """, namespaceCode, env, displayName, description, directoryMode.name(), now, now);
        } else {
            requireNamespace(request.getId());
            jdbcTemplate.update("""
                    UPDATE pyin_plugin_config_namespace
                    SET namespace_code = ?, env = ?, display_name = ?, description = ?, directory_mode = ?, updated_at = ?
                    WHERE id = ?
                    """, namespaceCode, env, displayName, description, directoryMode.name(), now, request.getId());
        }
        return Map.of("saved", true);
    }

    @Transactional
    public void deleteNamespace(Long id) {
        requireNamespace(id);
        requireCountZero("SELECT COUNT(*) FROM pyin_plugin_config_item WHERE namespace_id = ?", id, "该命名空间下仍有配置项，无法删除。");
        requireCountZero("SELECT COUNT(*) FROM pyin_plugin_config_directory WHERE namespace_id = ?", id, "该命名空间下仍有目录，无法删除。");
        jdbcTemplate.update("DELETE FROM pyin_plugin_config_namespace WHERE id = ?", id);
    }

    public List<Map<String, Object>> listItems(Long namespaceId, String keyword, Long directoryId) {
        String normalizedKeyword = optionalText(keyword, 160, "keyword");
        if (namespaceId == null) {
            if (directoryId != null) {
                throw invalid("directoryId 只能与 namespaceId 一起使用。");
            }
            String where = normalizedKeyword == null ? "" : "WHERE item.item_key LIKE ? OR item.item_value LIKE ? OR item.description LIKE ?";
            Object[] args = normalizedKeyword == null ? new Object[0] : new Object[]{like(normalizedKeyword), like(normalizedKeyword), like(normalizedKeyword)};
            return jdbcTemplate.query(itemSelect(where, "ORDER BY ns.namespace_code, ns.env, item.item_key"), ITEM_ROW_MAPPER, args);
        }
        Map<String, Object> namespace = requireNamespace(namespaceId);
        ConfigDirectoryMode mode = ConfigDirectoryMode.from((String) namespace.get("directoryMode"));
        if (directoryId != null) {
            requireDirectoryInNamespace(directoryId, namespaceId);
        }
        StringBuilder where = new StringBuilder("WHERE item.namespace_id = ?");
        List<Object> args = new ArrayList<>();
        args.add(namespaceId);
        if (directoryId != null) {
            where.append(" AND item.directory_id = ?");
            args.add(directoryId);
        }
        if (normalizedKeyword != null) {
            where.append(" AND (item.item_key LIKE ? OR item.item_value LIKE ? OR item.description LIKE ?)");
            args.add(like(normalizedKeyword));
            args.add(like(normalizedKeyword));
            args.add(like(normalizedKeyword));
        }
        if (mode == ConfigDirectoryMode.KEY_PROJECTION && directoryId != null) {
            throw invalid("当前命名空间使用配置键投影模式，无法按目录筛选。");
        }
        return jdbcTemplate.query(itemSelect(where.toString(), "ORDER BY item.item_key"), ITEM_ROW_MAPPER, args.toArray());
    }

    public Map<String, Object> getItem(Long id) {
        List<Map<String, Object>> rows = jdbcTemplate.query(itemSelect("WHERE item.id = ?", ""), ITEM_ROW_MAPPER, id);
        if (rows.isEmpty()) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "配置项不存在。");
        }
        return rows.get(0);
    }

    @Transactional
    public Map<String, Object> saveItem(ConfigItemSaveRequest request) {
        if (request.getNamespaceId() == null) {
            throw invalid("namespaceId 不能为空。");
        }
        Map<String, Object> namespace = requireNamespace(request.getNamespaceId());
        ConfigDirectoryMode directoryMode = ConfigDirectoryMode.from((String) namespace.get("directoryMode"));
        String itemKey = validateItemKey(request.getItemKey());
        ConfigValueType valueType = ConfigValueType.from(request.getValueType());
        String status = validateStatus(request.getStatus());
        String itemValue = request.getItemValue() == null || request.getItemValue().isBlank()
                ? null : validateValue(valueType, request.getItemValue());
        String defaultValue = request.getDefaultValue() == null || request.getDefaultValue().isBlank()
                ? null : validateValue(valueType, request.getDefaultValue());
        String description = optionalText(request.getDescription(), 512, "description");
        if (directoryMode == ConfigDirectoryMode.KEY_PROJECTION && request.getDirectoryId() != null) {
            throw invalid("配置键投影模式不支持配置项目录归属。");
        }
        if (request.getDirectoryId() != null) {
            requireDirectoryInNamespace(request.getDirectoryId(), request.getNamespaceId());
        }
        Long existingId = jdbcTemplate.query("SELECT id FROM pyin_plugin_config_item WHERE namespace_id = ? AND item_key = ?",
                resultSet -> resultSet.next() ? resultSet.getLong("id") : null, request.getNamespaceId(), itemKey);
        if (request.getId() == null && existingId != null) {
            throw invalid("当前空间已存在此 Key。");
        }
        if (request.getId() != null && existingId != null && !request.getId().equals(existingId)) {
            throw invalid("配置键已被其他记录占用。");
        }
        Timestamp now = Timestamp.from(Instant.now());
        if (request.getId() == null) {
            jdbcTemplate.update("""
                    INSERT INTO pyin_plugin_config_item
                        (namespace_id, directory_id, item_key, item_value, default_value, value_type, status, description, created_at, updated_at)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """, request.getNamespaceId(), request.getDirectoryId(), itemKey, itemValue, defaultValue, valueType.name(), status, description, now, now);
        } else {
            requireItemExists(request.getId());
            jdbcTemplate.update("""
                    UPDATE pyin_plugin_config_item
                    SET namespace_id = ?, directory_id = ?, item_key = ?, item_value = ?, default_value = ?, value_type = ?, status = ?, description = ?, updated_at = ?
                    WHERE id = ?
                    """, request.getNamespaceId(), request.getDirectoryId(), itemKey, itemValue, defaultValue, valueType.name(), status, description, now, request.getId());
        }
        return Map.of("saved", true);
    }

    @Transactional
    public void deleteItem(Long id) {
        requireItemExists(id);
        jdbcTemplate.update("DELETE FROM pyin_plugin_config_item WHERE id = ?", id);
    }

    public List<ConfigDirectoryTreeNode> listDirectoryTree(Long namespaceId) {
        requireDirectoryMode(namespaceId);
        List<ConfigDirectoryTreeNode> directories = jdbcTemplate.query("""
                SELECT directory.id, directory.parent_id, directory.name, directory.description, directory.sort_order,
                       COUNT(item.id) AS item_count
                FROM pyin_plugin_config_directory directory
                LEFT JOIN pyin_plugin_config_item item ON item.directory_id = directory.id
                WHERE directory.namespace_id = ?
                GROUP BY directory.id, directory.parent_id, directory.name, directory.description, directory.sort_order
                ORDER BY directory.sort_order, directory.name
                """, (resultSet, rowNum) -> {
            ConfigDirectoryTreeNode node = new ConfigDirectoryTreeNode();
            node.setId(resultSet.getLong("id"));
            node.setParentId(resultSet.getObject("parent_id", Long.class));
            node.setName(resultSet.getString("name"));
            node.setDescription(resultSet.getString("description"));
            node.setSortOrder(resultSet.getInt("sort_order"));
            node.setItemCount(resultSet.getInt("item_count"));
            return node;
        }, namespaceId);
        Map<Long, ConfigDirectoryTreeNode> byId = new LinkedHashMap<>();
        directories.forEach(node -> byId.put(node.getId(), node));
        List<ConfigDirectoryTreeNode> roots = new ArrayList<>();
        for (ConfigDirectoryTreeNode node : directories) {
            if (node.getParentId() == null) {
                roots.add(node);
            } else {
                ConfigDirectoryTreeNode parent = byId.get(node.getParentId());
                if (parent != null) {
                    parent.getChildren().add(node);
                }
            }
        }
        return roots;
    }

    @Transactional
    public Map<String, Object> saveDirectory(ConfigDirectorySaveRequest request) {
        if (request.getNamespaceId() == null) {
            throw invalid("namespaceId 不能为空。");
        }
        requireDirectoryMode(request.getNamespaceId());
        String name = requireText(request.getName(), "name", 128);
        if (!DIRECTORY_NAME_PATTERN.matcher(name).matches()) {
            throw invalid("目录名称只能包含字母、数字、点、下划线和短横线。");
        }
        if (request.getParentId() != null) {
            requireDirectoryInNamespace(request.getParentId(), request.getNamespaceId());
        }
        if (hasSiblingName(request.getNamespaceId(), request.getParentId(), name, request.getId())) {
            throw invalid("同级目录名称已存在。");
        }
        String description = optionalText(request.getDescription(), 512, "description");
        int sortOrder = request.getSortOrder() == null ? 0 : request.getSortOrder();
        Timestamp now = Timestamp.from(Instant.now());
        if (request.getId() == null) {
            jdbcTemplate.update("""
                    INSERT INTO pyin_plugin_config_directory
                        (namespace_id, parent_id, name, description, sort_order, created_at, updated_at)
                    VALUES (?, ?, ?, ?, ?, ?, ?)
                    """, request.getNamespaceId(), request.getParentId(), name, description, sortOrder, now, now);
        } else {
            Map<String, Object> directory = requireDirectory(request.getId());
            if (!Objects.equals(directory.get("namespaceId"), request.getNamespaceId())) {
                throw invalid("目录不能移动到其他命名空间。");
            }
            if (request.getParentId() != null && request.getParentId().equals(request.getId())) {
                throw invalid("目录不能以自身作为父目录。");
            }
            ensureNotDescendant(request.getId(), request.getParentId());
            jdbcTemplate.update("""
                    UPDATE pyin_plugin_config_directory
                    SET parent_id = ?, name = ?, description = ?, sort_order = ?, updated_at = ?
                    WHERE id = ?
                    """, request.getParentId(), name, description, sortOrder, now, request.getId());
        }
        return Map.of("saved", true);
    }

    @Transactional
    public void moveDirectory(Long id, ConfigDirectoryMoveRequest request) {
        Map<String, Object> directory = requireDirectory(id);
        Long namespaceId = (Long) directory.get("namespaceId");
        requireDirectoryMode(namespaceId);
        if (request.getParentId() != null) {
            requireDirectoryInNamespace(request.getParentId(), namespaceId);
            ensureNotDescendant(id, request.getParentId());
        }
        String name = (String) directory.get("name");
        if (hasSiblingName(namespaceId, request.getParentId(), name, id)) {
            throw invalid("目标目录下已存在同名目录。");
        }
        jdbcTemplate.update("UPDATE pyin_plugin_config_directory SET parent_id = ?, sort_order = ?, updated_at = ? WHERE id = ?",
                request.getParentId(), request.getSortOrder() == null ? directory.get("sortOrder") : request.getSortOrder(), Timestamp.from(Instant.now()), id);
    }

    @Transactional
    public void deleteDirectory(Long id) {
        Map<String, Object> directory = requireDirectory(id);
        requireDirectoryMode((Long) directory.get("namespaceId"));
        requireCountZero("SELECT COUNT(*) FROM pyin_plugin_config_directory WHERE parent_id = ?", id, "目录下仍有子目录，无法删除。");
        requireCountZero("SELECT COUNT(*) FROM pyin_plugin_config_item WHERE directory_id = ?", id, "目录下仍有配置项，无法删除。");
        jdbcTemplate.update("DELETE FROM pyin_plugin_config_directory WHERE id = ?", id);
    }

    private String itemSelect(String where, String orderBy) {
        return """
                SELECT item.id, item.namespace_id, item.directory_id, ns.namespace_code, ns.env, ns.display_name,
                       item.item_key, item.item_value, item.default_value, item.value_type, item.status,
                       item.description, item.created_at, item.updated_at
                FROM pyin_plugin_config_item item
                JOIN pyin_plugin_config_namespace ns ON ns.id = item.namespace_id
                """ + where + " " + orderBy;
    }

    private Map<String, Object> requireNamespace(Long id) {
        List<Map<String, Object>> namespaces = jdbcTemplate.query("""
                SELECT id, namespace_code, env, display_name, description, directory_mode,
                       created_at, updated_at, 0 AS item_count
                FROM pyin_plugin_config_namespace WHERE id = ?
                """, NAMESPACE_ROW_MAPPER, id);
        if (namespaces.isEmpty()) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "命名空间不存在。");
        }
        return namespaces.get(0);
    }

    private void requireDirectoryMode(Long namespaceId) {
        Map<String, Object> namespace = requireNamespace(namespaceId);
        if (ConfigDirectoryMode.from((String) namespace.get("directoryMode")) != ConfigDirectoryMode.DIRECTORY_API) {
            throw invalid("当前命名空间使用配置键投影模式，无法维护持久化目录。");
        }
    }

    private Map<String, Object> requireDirectory(Long id) {
        List<Map<String, Object>> rows = jdbcTemplate.query("""
                SELECT id, namespace_id, parent_id, name, description, sort_order
                FROM pyin_plugin_config_directory WHERE id = ?
                """, (resultSet, rowNum) -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", resultSet.getLong("id"));
            row.put("namespaceId", resultSet.getLong("namespace_id"));
            row.put("parentId", resultSet.getObject("parent_id", Long.class));
            row.put("name", resultSet.getString("name"));
            row.put("sortOrder", resultSet.getInt("sort_order"));
            return row;
        }, id);
        if (rows.isEmpty()) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "配置目录不存在。");
        }
        return rows.get(0);
    }

    private void requireDirectoryInNamespace(Long id, Long namespaceId) {
        Map<String, Object> directory = requireDirectory(id);
        if (!Objects.equals(directory.get("namespaceId"), namespaceId)) {
            throw invalid("目录不属于当前命名空间。");
        }
    }

    private boolean hasSiblingName(Long namespaceId, Long parentId, String name, Long excludedId) {
        String sql = "SELECT COUNT(*) FROM pyin_plugin_config_directory WHERE namespace_id = ? AND "
                + (parentId == null ? "parent_id IS NULL" : "parent_id = ?") + " AND name = ?"
                + (excludedId == null ? "" : " AND id <> ?");
        List<Object> args = new ArrayList<>();
        args.add(namespaceId);
        if (parentId != null) args.add(parentId);
        args.add(name);
        if (excludedId != null) args.add(excludedId);
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, args.toArray());
        return count != null && count > 0;
    }

    private void ensureNotDescendant(Long id, Long parentId) {
        Long current = parentId;
        while (current != null) {
            if (id.equals(current)) {
                throw invalid("目录不能移动到自身或子目录下。");
            }
            current = (Long) requireDirectory(current).get("parentId");
        }
    }

    private String validateItemKey(String value) {
        String key = requireText(value, "itemKey", 160);
        if (!ITEM_KEY_PATTERN.matcher(key).matches()) {
            throw invalid("配置键可直接填写，或使用冒号分隔层级；每段以小写字母或数字开头。");
        }
        return key;
    }

    private String validateValue(ConfigValueType valueType, String value) {
        if (value == null || value.isBlank()) {
            throw invalid("itemValue 不能为空。");
        }
        return switch (valueType) {
            case STRING -> value;
            case INTEGER -> validateInteger(value);
            case BOOLEAN -> validateBoolean(value);
            case JSON -> validateJson(value);
        };
    }

    private String validateInteger(String value) {
        if (!INTEGER_PATTERN.matcher(value).matches()) {
            throw invalid("INTEGER 配置值必须是严格的 64 位十进制整数。");
        }
        try {
            Long.parseLong(value);
            return value;
        } catch (NumberFormatException exception) {
            throw invalid("INTEGER 配置值超出 64 位整数范围。");
        }
    }

    private String validateBoolean(String value) {
        if (!"true".equals(value) && !"false".equals(value)) {
            throw invalid("BOOLEAN 配置值只能是 true 或 false。");
        }
        return value;
    }

    private String validateJson(String value) {
        try {
            JsonNode json = objectMapper.readTree(value);
            if (json == null || (!json.isObject() && !json.isArray())) {
                throw invalid("JSON 配置值只能是对象或数组。");
            }
            return value;
        } catch (JsonProcessingException exception) {
            throw invalid("JSON 配置值格式不正确。");
        }
    }

    private String validateStatus(String value) {
        if (value == null || value.isBlank()) {
            return "ENABLED";
        }
        String status = value.trim().toUpperCase(java.util.Locale.ROOT);
        if (!"ENABLED".equals(status) && !"DISABLED".equals(status)) {
            throw invalid("配置状态只能是 ENABLED 或 DISABLED。");
        }
        return status;
    }

    private void requireItemExists(Long id) {
        requireCountAtLeastOne("SELECT COUNT(*) FROM pyin_plugin_config_item WHERE id = ?", id, "配置项不存在。");
    }

    private void requireCountAtLeastOne(String sql, Object id, String message) {
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, id);
        if (count == null || count == 0) {
            throw new BusinessException(ErrorCode.NOT_FOUND, message);
        }
    }

    private void requireCountZero(String sql, Object id, String message) {
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, id);
        if (count != null && count > 0) {
            throw invalid(message);
        }
    }

    private void ensureColumn(String table, String column, String definition) {
        Boolean exists = jdbcTemplate.execute((ConnectionCallback<Boolean>) connection -> {
            DatabaseMetaData metadata = connection.getMetaData();
            try (ResultSet columns = metadata.getColumns(null, null, "%", "%")) {
                while (columns.next()) {
                    if (table.equalsIgnoreCase(columns.getString("TABLE_NAME")) && column.equalsIgnoreCase(columns.getString("COLUMN_NAME"))) {
                        return true;
                    }
                }
                return false;
            }
        });
        if (!Boolean.TRUE.equals(exists)) {
            jdbcTemplate.execute("ALTER TABLE " + table + " ADD COLUMN " + column + " " + definition);
        }
    }

    private void allowNullItemValue() {
        jdbcTemplate.execute((ConnectionCallback<Void>) connection -> {
            String databaseProduct = connection.getMetaData().getDatabaseProductName();
            String statement = "MySQL".equalsIgnoreCase(databaseProduct)
                    ? "ALTER TABLE pyin_plugin_config_item MODIFY COLUMN item_value VARCHAR(4000) NULL"
                    : "H2".equalsIgnoreCase(databaseProduct)
                            ? "ALTER TABLE pyin_plugin_config_item ALTER COLUMN item_value SET NULL"
                            : "ALTER TABLE pyin_plugin_config_item ALTER COLUMN item_value DROP NOT NULL";
            try (var sqlStatement = connection.createStatement()) {
                sqlStatement.execute(statement);
            }
            return null;
        });
    }

    private void ensureIndex(String name, String table, String columns) {
        Boolean exists = jdbcTemplate.execute((ConnectionCallback<Boolean>) connection -> {
            DatabaseMetaData metadata = connection.getMetaData();
            for (String tableName : List.of(table, table.toUpperCase(java.util.Locale.ROOT))) {
                try (ResultSet indexes = metadata.getIndexInfo(
                        connection.getCatalog(),
                        connection.getSchema(),
                        tableName,
                        false,
                        false
                )) {
                    while (indexes.next()) {
                        if (name.equalsIgnoreCase(indexes.getString("INDEX_NAME"))) {
                            return true;
                        }
                    }
                }
            }
            return false;
        });
        if (!Boolean.TRUE.equals(exists)) {
            Boolean h2Database = jdbcTemplate.execute((ConnectionCallback<Boolean>) connection ->
                    "H2".equalsIgnoreCase(connection.getMetaData().getDatabaseProductName())
            );
            String createIndex = Boolean.TRUE.equals(h2Database)
                    ? "CREATE INDEX IF NOT EXISTS "
                    : "CREATE INDEX ";
            jdbcTemplate.execute(createIndex + name + " ON " + table + " (" + columns + ")");
        }
    }

    private void seedDemoData() {
        Integer namespaceCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM pyin_plugin_config_namespace", Integer.class);
        if (namespaceCount != null && namespaceCount > 0) return;
        Timestamp now = Timestamp.from(Instant.now());
        jdbcTemplate.update("""
                INSERT INTO pyin_plugin_config_namespace
                    (namespace_code, env, display_name, description, directory_mode, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """, "order-service", "prod", "订单服务生产环境", "管理订单服务在生产环境下的关键参数。", ConfigDirectoryMode.KEY_PROJECTION.name(), now, now);
        jdbcTemplate.update("""
                INSERT INTO pyin_plugin_config_namespace
                    (namespace_code, env, display_name, description, directory_mode, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """, "order-service", "test", "订单服务测试环境", "用于测试环境联调的命名空间。", ConfigDirectoryMode.KEY_PROJECTION.name(), now, now);
        Long prodId = jdbcTemplate.queryForObject("SELECT id FROM pyin_plugin_config_namespace WHERE namespace_code = ? AND env = ?", Long.class, "order-service", "prod");
        Long testId = jdbcTemplate.queryForObject("SELECT id FROM pyin_plugin_config_namespace WHERE namespace_code = ? AND env = ?", Long.class, "order-service", "test");
        insertDemoItem(prodId, "order:timeout:seconds", "45", "INTEGER", "订单超时时间。", now);
        insertDemoItem(prodId, "order:notify:enabled", "true", "BOOLEAN", "是否开启订单通知。", now);
        insertDemoItem(testId, "order:mock:mode", "sandbox", "STRING", "测试环境模拟模式。", now);
    }

    private void insertDemoItem(Long namespaceId, String key, String value, String type, String description, Timestamp now) {
        jdbcTemplate.update("""
                INSERT INTO pyin_plugin_config_item
                    (namespace_id, directory_id, item_key, item_value, value_type, description, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """, namespaceId, null, key, value, type, description, now, now);
    }

    private static String requireText(String value, String field, int maxLength) {
        String normalized = value == null ? null : value.trim();
        if (!StringUtils.hasText(normalized)) throw invalid(field + " 不能为空。");
        if (normalized.length() > maxLength) throw invalid(field + " 超过长度限制。");
        return normalized;
    }

    private static String optionalText(String value, int maxLength, String field) {
        if (value == null || value.trim().isEmpty()) return null;
        if (value.length() > maxLength) throw invalid(field + " 超过长度限制。");
        return value;
    }

    private static String like(String keyword) { return "%" + keyword + "%"; }
    private static BusinessException invalid(String message) { return new BusinessException(ErrorCode.INVALID_REQUEST, message); }
    private static String asIsoString(ResultSet resultSet, String column) throws SQLException {
        Timestamp timestamp = resultSet.getTimestamp(column);
        return timestamp == null ? null : timestamp.toInstant().toString();
    }
}
