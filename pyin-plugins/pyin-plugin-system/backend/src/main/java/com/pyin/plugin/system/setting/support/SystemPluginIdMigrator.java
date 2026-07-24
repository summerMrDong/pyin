package com.pyin.plugin.system.setting.support;

import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.DependsOn;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 将历史系统插件运行时标识迁移为当前的 {@code system} 标识。
 */
@Component
@DependsOn("coreSchemaInitializer")
public class SystemPluginIdMigrator {

    static final String LEGACY_PLUGIN_ID = "pyin-system";
    static final String SYSTEM_PLUGIN_ID = "system";
    private static final String PLUGIN_SCOPE = "PLUGIN";

    private final JdbcTemplate jdbcTemplate;
    private final TransactionTemplate transactionTemplate;

    public SystemPluginIdMigrator(JdbcTemplate jdbcTemplate, TransactionTemplate transactionTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.transactionTemplate = transactionTemplate;
    }

    @PostConstruct
    public void migrate() {
        transactionTemplate.executeWithoutResult(status -> migrateMetadata());
    }

    private void migrateMetadata() {
        jdbcTemplate.update(
                "UPDATE pyin_role_resource SET resource_code = ? || SUBSTRING(resource_code, ?) "
                        + "WHERE resource_scope = ? AND resource_code LIKE ?",
                SYSTEM_PLUGIN_ID + "/",
                LEGACY_PLUGIN_ID.length() + 2,
                PLUGIN_SCOPE,
                LEGACY_PLUGIN_ID + "/%"
        );
        deletePluginMetadata("pyin_plugin_permission");
        deletePluginMetadata("pyin_plugin_api");
        deletePluginMetadata("pyin_plugin_resource");
        jdbcTemplate.update("DELETE FROM pyin_plugin WHERE plugin_id = ?", LEGACY_PLUGIN_ID);
    }

    private void deletePluginMetadata(String tableName) {
        jdbcTemplate.update("DELETE FROM " + tableName + " WHERE plugin_id = ?", LEGACY_PLUGIN_ID);
    }
}
