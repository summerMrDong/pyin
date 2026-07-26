package com.pyin.plugin.system.setting.support;

import com.pyin.plugin.system.user.service.UserService;
import jakarta.annotation.PostConstruct;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.security.crypto.password.PasswordEncoder;

@Component
public class CoreSchemaInitializer {

    public static final String DEFAULT_ADMIN_PASSWORD = "123456";
    private static final String LEGACY_DEFAULT_ADMIN_PASSWORD = "Admin@123456";

    private final JdbcTemplate jdbcTemplate;
    private final PasswordEncoder passwordEncoder;

    public CoreSchemaInitializer(JdbcTemplate jdbcTemplate, PasswordEncoder passwordEncoder) {
        this.jdbcTemplate = jdbcTemplate;
        this.passwordEncoder = passwordEncoder;
    }

    @PostConstruct
    public void initialize() {
        createTables();
        seedDefaults();
    }

    private void createTables() {
        execute("""
                CREATE TABLE IF NOT EXISTS pyin_user (
                    id BIGINT PRIMARY KEY,
                    username VARCHAR(128) NOT NULL,
                    display_name VARCHAR(128),
                    password_hash VARCHAR(128),
                    status VARCHAR(32),
                    created_at TIMESTAMP,
                    updated_at TIMESTAMP
                )
                """);
        execute("ALTER TABLE pyin_user ADD COLUMN IF NOT EXISTS password_hash VARCHAR(128)");
        execute("ALTER TABLE pyin_user ADD COLUMN IF NOT EXISTS status VARCHAR(32)");
        execute("ALTER TABLE pyin_user ADD COLUMN IF NOT EXISTS created_at TIMESTAMP");
        execute("ALTER TABLE pyin_user ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP");
        execute("""
                CREATE TABLE IF NOT EXISTS pyin_role (
                    id BIGINT PRIMARY KEY,
                    code VARCHAR(128) NOT NULL,
                    name VARCHAR(128) NOT NULL,
                    description VARCHAR(255),
                    sort INT DEFAULT 0,
                    created_at TIMESTAMP,
                    updated_at TIMESTAMP
                )
                """);
        execute("ALTER TABLE pyin_role ADD COLUMN IF NOT EXISTS description VARCHAR(255)");
        execute("ALTER TABLE pyin_role ADD COLUMN IF NOT EXISTS sort INT DEFAULT 0");
        execute("ALTER TABLE pyin_role ADD COLUMN IF NOT EXISTS created_at TIMESTAMP");
        execute("ALTER TABLE pyin_role ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP");
        execute("""
                CREATE TABLE IF NOT EXISTS pyin_permission (
                    id BIGINT PRIMARY KEY,
                    code VARCHAR(128) NOT NULL,
                    name VARCHAR(128) NOT NULL
                )
                """);
        execute("""
                CREATE TABLE IF NOT EXISTS pyin_user_role (
                    id BIGINT PRIMARY KEY,
                    user_id BIGINT NOT NULL,
                    role_id BIGINT NOT NULL
                )
                """);
        execute("""
                CREATE TABLE IF NOT EXISTS pyin_role_permission (
                    id BIGINT PRIMARY KEY,
                    role_id BIGINT NOT NULL,
                    permission_code VARCHAR(128) NOT NULL
                )
                """);
        execute("""
                CREATE TABLE IF NOT EXISTS pyin_role_resource (
                    id BIGINT PRIMARY KEY,
                    role_id BIGINT NOT NULL,
                    resource_code VARCHAR(255) NOT NULL,
                    resource_scope VARCHAR(64) NOT NULL,
                    created_at TIMESTAMP
                )
                """);
        execute("""
                CREATE TABLE IF NOT EXISTS pyin_plugin (
                    plugin_id VARCHAR(128) PRIMARY KEY,
                    plugin_name VARCHAR(128) NOT NULL,
                    version VARCHAR(64)
                )
                """);
        execute("""
                CREATE TABLE IF NOT EXISTS pyin_plugin_permission (
                    id BIGINT PRIMARY KEY,
                    plugin_id VARCHAR(128) NOT NULL,
                    permission_code VARCHAR(128) NOT NULL,
                    permission_name VARCHAR(128) NOT NULL,
                    resource_type VARCHAR(64) NOT NULL
                )
                """);
        execute("""
                CREATE TABLE IF NOT EXISTS pyin_plugin_api (
                    id BIGINT PRIMARY KEY,
                    plugin_id VARCHAR(128) NOT NULL,
                    path VARCHAR(255) NOT NULL,
                    method VARCHAR(16) NOT NULL,
                    access_mode VARCHAR(64) NOT NULL,
                    permission_code VARCHAR(128),
                    audit_enabled BOOLEAN NOT NULL
                )
                """);
        execute("""
                CREATE TABLE IF NOT EXISTS pyin_plugin_resource (
                    id BIGINT PRIMARY KEY,
                    plugin_id VARCHAR(128) NOT NULL,
                    resource_code VARCHAR(255) NOT NULL,
                    resource_name VARCHAR(255) NOT NULL,
                    resource_type VARCHAR(64) NOT NULL,
                    parent_code VARCHAR(255),
                    path VARCHAR(255),
                    icon VARCHAR(128),
                    sort INT,
                    permission_code VARCHAR(128),
                    visible BOOLEAN NOT NULL,
                    metadata_json CLOB
                )
                """);
        execute("""
                CREATE TABLE IF NOT EXISTS pyin_client_credential (
                    id BIGINT PRIMARY KEY,
                    credential_name VARCHAR(128),
                    access_key VARCHAR(128),
                    access_secret_hash VARCHAR(255),
                    access_secret_encrypted CLOB,
                    status VARCHAR(64),
                    created_at TIMESTAMP,
                    updated_at TIMESTAMP
                )
                """);
        execute("ALTER TABLE pyin_client_credential ADD COLUMN IF NOT EXISTS credential_name VARCHAR(128)");
        execute("ALTER TABLE pyin_client_credential ADD COLUMN IF NOT EXISTS access_secret_hash VARCHAR(255)");
        execute("ALTER TABLE pyin_client_credential ADD COLUMN IF NOT EXISTS access_secret_encrypted CLOB");
        execute("ALTER TABLE pyin_client_credential ADD COLUMN IF NOT EXISTS created_at TIMESTAMP");
        execute("ALTER TABLE pyin_client_credential ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP");
        execute("""
                CREATE TABLE IF NOT EXISTS pyin_client_request_log (
                    id BIGINT PRIMARY KEY,
                    credential_id BIGINT,
                    access_key VARCHAR(128),
                    request_type VARCHAR(64),
                    request_uri VARCHAR(255),
                    http_method VARCHAR(16),
                    client_ip VARCHAR(64),
                    request_status VARCHAR(32),
                    failure_code VARCHAR(128),
                    failure_message VARCHAR(255),
                    created_at TIMESTAMP
                )
                """);
        execute("""
                CREATE TABLE IF NOT EXISTS pyin_audit_log (
                    id BIGINT PRIMARY KEY,
                    action VARCHAR(255),
                    operator_name VARCHAR(128)
                )
                """);
        execute("""
                CREATE TABLE IF NOT EXISTS pyin_system_setting (
                    id BIGINT PRIMARY KEY,
                    setting_key VARCHAR(128),
                    setting_value CLOB
                )
                """);
    }

    private void seedDefaults() {
        String adminPasswordHash = passwordEncoder.encode(DEFAULT_ADMIN_PASSWORD);
        insertWhenMissing(
                "SELECT COUNT(*) FROM pyin_user WHERE id = 1",
                "INSERT INTO pyin_user(id, username, display_name, password_hash, status, created_at, updated_at) " +
                        "VALUES (1, 'admin', 'Pyin Admin', '%s', '%s', current_timestamp, current_timestamp)"
                                .formatted(adminPasswordHash, UserService.STATUS_ENABLED)
        );
        updateWhenMissing(
                "SELECT COUNT(*) FROM pyin_user WHERE id = 1 AND (password_hash IS NULL OR password_hash = '')",
                "UPDATE pyin_user SET password_hash = '%s' WHERE id = 1".formatted(adminPasswordHash)
        );
        migrateLegacyDefaultAdminPassword(adminPasswordHash);
        updateWhenMissing(
                "SELECT COUNT(*) FROM pyin_user WHERE id = 1 AND (status IS NULL OR status = '')",
                "UPDATE pyin_user SET status = '%s' WHERE id = 1".formatted(UserService.STATUS_ENABLED)
        );
        updateWhenMissing(
                "SELECT COUNT(*) FROM pyin_user WHERE id = 1 AND created_at IS NULL",
                "UPDATE pyin_user SET created_at = current_timestamp WHERE id = 1"
        );
        updateWhenMissing(
                "SELECT COUNT(*) FROM pyin_user WHERE id = 1 AND updated_at IS NULL",
                "UPDATE pyin_user SET updated_at = current_timestamp WHERE id = 1"
        );
        insertWhenMissing(
                "SELECT COUNT(*) FROM pyin_role WHERE id = 1",
                "INSERT INTO pyin_role(id, code, name, description, created_at, updated_at) " +
                        "VALUES (1, 'ADMIN', '管理员', '平台默认超级管理员角色', current_timestamp, current_timestamp)"
        );
        updateWhenMissing(
                "SELECT COUNT(*) FROM pyin_role WHERE id = 1 AND created_at IS NULL",
                "UPDATE pyin_role SET created_at = current_timestamp WHERE id = 1"
        );
        updateWhenMissing(
                "SELECT COUNT(*) FROM pyin_role WHERE id = 1 AND updated_at IS NULL",
                "UPDATE pyin_role SET updated_at = current_timestamp WHERE id = 1"
        );
        updateWhenMissing(
                "SELECT COUNT(*) FROM pyin_role WHERE id = 1 AND sort IS NULL",
                "UPDATE pyin_role SET sort = 0 WHERE id = 1"
        );
        insertWhenMissing("SELECT COUNT(*) FROM pyin_user_role WHERE user_id = 1 AND role_id = 1", "INSERT INTO pyin_user_role(id, user_id, role_id) VALUES (1001, 1, 1)");
        seedPermission(2001, "system:view", "系统查看");
        seedPermission(2002, "user:view", "用户查看");
        seedPermission(2003, "user:create", "用户创建");
        seedPermission(2004, "user:update", "用户更新");
        seedPermission(2005, "user:delete", "用户删除");
        seedPermission(2006, "user:reset-password", "用户重置密码");
        seedPermission(2007, "role:view", "角色查看");
        seedPermission(2008, "role:create", "角色创建");
        seedPermission(2009, "role:update", "角色更新");
        seedPermission(2010, "role:delete", "角色删除");
        seedPermission(2011, "credential:view", "接入凭证查看");
        seedPermission(2012, "credential:create", "接入凭证创建");
        seedPermission(2013, "credential:update", "接入凭证更新");
        seedPermission(2014, "credential:rotate-secret", "接入凭证轮换密钥");
        seedPermission(2015, "credential:view-logs", "接入凭证查看日志");
        seedRolePermission(3001, 1L, "system:view");
        seedRolePermission(3002, 1L, "user:view");
        seedRolePermission(3003, 1L, "user:create");
        seedRolePermission(3004, 1L, "user:update");
        seedRolePermission(3005, 1L, "user:delete");
        seedRolePermission(3006, 1L, "user:reset-password");
        seedRolePermission(3007, 1L, "role:view");
        seedRolePermission(3008, 1L, "role:create");
        seedRolePermission(3009, 1L, "role:update");
        seedRolePermission(3010, 1L, "role:delete");
        seedRolePermission(3011, 1L, "credential:view");
        seedRolePermission(3012, 1L, "credential:create");
        seedRolePermission(3013, 1L, "credential:update");
        seedRolePermission(3014, 1L, "credential:rotate-secret");
        seedRolePermission(3015, 1L, "credential:view-logs");
    }

    private void migrateLegacyDefaultAdminPassword(String adminPasswordHash) {
        String currentPasswordHash = jdbcTemplate.queryForObject(
                "SELECT password_hash FROM pyin_user WHERE id = 1",
                String.class
        );
        if (currentPasswordHash != null
                && passwordEncoder.matches(LEGACY_DEFAULT_ADMIN_PASSWORD, currentPasswordHash)) {
            jdbcTemplate.update(
                    "UPDATE pyin_user SET password_hash = ?, updated_at = current_timestamp WHERE id = 1",
                    adminPasswordHash
            );
        }
    }

    private void seedPermission(long id, String code, String name) {
        insertWhenMissing(
                "SELECT COUNT(*) FROM pyin_permission WHERE code = '%s'".formatted(code),
                "INSERT INTO pyin_permission(id, code, name) VALUES (%d, '%s', '%s')".formatted(id, code, name)
        );
    }

    private void seedRolePermission(long id, long roleId, String permissionCode) {
        insertWhenMissing(
                "SELECT COUNT(*) FROM pyin_role_permission WHERE role_id = %d AND permission_code = '%s'"
                        .formatted(roleId, permissionCode),
                "INSERT INTO pyin_role_permission(id, role_id, permission_code) VALUES (%d, %d, '%s')"
                        .formatted(id, roleId, permissionCode)
        );
    }

    private void insertWhenMissing(String countSql, String insertSql) {
        Integer count = jdbcTemplate.queryForObject(countSql, Integer.class);
        if (count == null || count == 0) {
            jdbcTemplate.execute(insertSql);
        }
    }

    private void updateWhenMissing(String countSql, String updateSql) {
        Integer count = jdbcTemplate.queryForObject(countSql, Integer.class);
        if (count != null && count > 0) {
            jdbcTemplate.execute(updateSql);
        }
    }

    private void execute(String sql) {
        jdbcTemplate.execute(sql);
    }
}
