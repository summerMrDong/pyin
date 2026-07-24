package com.pyin.plugin.bootstrap;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pyin.plugin.system.setting.support.CoreSchemaInitializer;
import com.pyin.plugin.system.setting.support.SystemPluginIdMigrator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:pyin-auth-test;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH",
        "spring.datasource.username=sa",
        "spring.datasource.password="
})
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private SystemPluginIdMigrator systemPluginIdMigrator;

    @Test
    void shouldStoreDefaultAdminPasswordWithBcrypt() {
        String passwordHash = jdbcTemplate.queryForObject(
                "SELECT password_hash FROM pyin_user WHERE id = 1",
                String.class
        );

        assertThat(passwordHash).startsWith("{bcrypt}");
    }

    @Test
    void shouldLoginWithDefaultAdminCredentials() throws Exception {
        String token = adminToken();

        MvcResult currentUserResult = mockMvc.perform(get("/api/auth/current-user")
                        .header(AUTHORIZATION, token))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode currentUserJson = json(currentUserResult);
        assertThat(currentUserJson.path("data").path("username").asText()).isEqualTo("admin");
        assertThat(currentUserJson.path("data").path("displayName").asText()).isEqualTo("Pyin Admin");
    }

    @Test
    void shouldRejectInvalidPassword() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"username":"admin","password":"bad-password"}
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldRequireLoginForCurrentUser() throws Exception {
        mockMvc.perform(get("/api/auth/current-user"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldRequireLoginForLogout() throws Exception {
        mockMvc.perform(post("/api/auth/logout"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldLogoutAndInvalidateCurrentSession() throws Exception {
        String token = adminToken();

        mockMvc.perform(post("/api/auth/logout")
                        .header(AUTHORIZATION, token))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/auth/current-user")
                        .header(AUTHORIZATION, token))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldRequireLoginForAdminApi() throws Exception {
        mockMvc.perform(get("/plugins/system/admin/client-credentials"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @Transactional
    void shouldAllowSuperAdminWithoutBoundPermission() throws Exception {
        jdbcTemplate.update(
                "DELETE FROM pyin_role_permission WHERE role_id = 1 AND permission_code = ?",
                "credential:view"
        );

        mockMvc.perform(get("/plugins/system/admin/client-credentials")
                        .header(AUTHORIZATION, adminToken()))
                .andExpect(status().isOk());
    }

    @Test
    void shouldMigrateLegacySystemPluginMetadataAndRoleResourcesIdempotently() {
        jdbcTemplate.update("INSERT INTO pyin_plugin(plugin_id, plugin_name, version) VALUES (?, ?, ?)",
                "pyin-system", "历史系统插件", "0.9.0");
        jdbcTemplate.update("INSERT INTO pyin_plugin_permission(id, plugin_id, permission_code, permission_name, resource_type) VALUES (?, ?, ?, ?, ?)",
                99001L, "pyin-system", "system:view", "系统查看", "API");
        jdbcTemplate.update("INSERT INTO pyin_plugin_api(id, plugin_id, path, method, access_mode, permission_code, audit_enabled) VALUES (?, ?, ?, ?, ?, ?, ?)",
                99002L, "pyin-system", "/plugins/pyin-system/admin/users", "GET", "ADMIN", "system:view", false);
        jdbcTemplate.update("INSERT INTO pyin_plugin_resource(id, plugin_id, resource_code, resource_name, resource_type, visible) VALUES (?, ?, ?, ?, ?, ?)",
                99003L, "pyin-system", "users", "用户管理", "PAGE", true);
        jdbcTemplate.update("INSERT INTO pyin_role_resource(id, role_id, resource_code, resource_scope, created_at) VALUES (?, ?, ?, ?, current_timestamp)",
                99004L, 1L, "pyin-system/users", "PLUGIN");

        systemPluginIdMigrator.migrate();
        systemPluginIdMigrator.migrate();

        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM pyin_plugin WHERE plugin_id = 'system'", Integer.class))
                .isGreaterThan(0);
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM pyin_plugin WHERE plugin_id = 'pyin-system'", Integer.class)).isZero();
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM pyin_plugin_permission WHERE plugin_id = 'pyin-system'", Integer.class)).isZero();
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM pyin_plugin_api WHERE plugin_id = 'pyin-system'", Integer.class)).isZero();
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM pyin_plugin_resource WHERE plugin_id = 'pyin-system'", Integer.class)).isZero();
        assertThat(jdbcTemplate.queryForObject("SELECT resource_code FROM pyin_role_resource WHERE id = 99004", String.class))
                .isEqualTo("system/users");
    }

    @Test
    void shouldAllowPluginNodeRegistrationWithoutAdminLogin() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/core/plugin-nodes/register")
                        .contentType(APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andReturn();

        assertThat(json(result).path("code").asText()).isEqualTo("PYIN-PLUGIN-400");
    }

    @Test
    void shouldRequireAdminLoginForCoreNotifyPublish() throws Exception {
        mockMvc.perform(post("/api/core/notify/publish")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"eventType":"config.changed","payload":{}}
                                """))
                .andExpect(status().isUnauthorized());
    }

    private String adminToken() throws Exception {
        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"username":"admin","password":"%s"}
                                """.formatted(CoreSchemaInitializer.DEFAULT_ADMIN_PASSWORD)))
                .andExpect(status().isOk())
                .andReturn();

        String token = json(loginResult).path("data").path("token").asText();
        assertThat(token).isNotBlank();
        return token;
    }

    private JsonNode json(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }
}
