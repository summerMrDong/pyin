package com.pyin.plugin.bootstrap;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pyin.plugin.system.setting.support.CoreSchemaInitializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:pyin-user-role-test;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH",
        "spring.datasource.username=sa",
        "spring.datasource.password="
})
class UserRoleManagementTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldCreateRoleAndUserThenResetPassword() throws Exception {
        String token = adminToken();

        MvcResult roleResult = mockMvc.perform(post("/plugins/system/admin/roles")
                        .header(AUTHORIZATION, token)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "code":"OPS_MANAGER",
                                  "name":"运维经理",
                                  "description":"负责系统运维和账号巡检",
                                  "permissionCodes":["user:view","role:view"]
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode roleJson = json(roleResult);
        assertThat(roleJson.path("success").asBoolean()).isTrue();
        long roleId = roleJson.path("data").path("id").asLong();

        MvcResult userResult = mockMvc.perform(post("/plugins/system/admin/users")
                        .header(AUTHORIZATION, token)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "username":"ops.lead",
                                  "displayName":"运维负责人",
                                  "password":"OpsLead@123",
                                  "status":"ENABLED",
                                  "roleIds":[%d]
                                }
                                """.formatted(roleId)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode userJson = json(userResult);
        assertThat(userJson.path("success").asBoolean()).isTrue();
        long userId = userJson.path("data").path("id").asLong();
        assertThat(userJson.path("data").path("roles")).hasSize(1);

        MvcResult listResult = mockMvc.perform(get("/plugins/system/admin/users")
                        .header(AUTHORIZATION, token)
                        .param("username", "ops.lead"))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode listJson = json(listResult);
        assertThat(listJson.path("data")).hasSize(1);
        assertThat(listJson.path("data").get(0).path("status").asText()).isEqualTo("ENABLED");

        mockMvc.perform(post("/plugins/system/admin/users/{id}/reset-password", userId)
                        .header(AUTHORIZATION, token)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"newPassword":"OpsLead@456"}
                                """))
                .andExpect(status().isOk());

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"username":"ops.lead","password":"OpsLead@456"}
                                """))
                .andExpect(status().isOk())
                .andReturn();

        assertThat(json(loginResult).path("data").path("token").asText()).isNotBlank();
    }

    @Test
    void shouldProtectDefaultAdminAndBoundRoles() throws Exception {
        String token = adminToken();

        MvcResult roleResult = mockMvc.perform(post("/plugins/system/admin/roles")
                        .header(AUTHORIZATION, token)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "code":"AUDITOR",
                                  "name":"审计员",
                                  "description":"只读审计角色",
                                  "permissionCodes":["system:view"]
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn();
        long roleId = json(roleResult).path("data").path("id").asLong();

        MvcResult userResult = mockMvc.perform(post("/plugins/system/admin/users")
                        .header(AUTHORIZATION, token)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "username":"audit.reader",
                                  "displayName":"审计账号",
                                  "password":"Audit@123",
                                  "status":"ENABLED",
                                  "roleIds":[%d]
                                }
                                """.formatted(roleId)))
                .andExpect(status().isOk())
                .andReturn();
        long userId = json(userResult).path("data").path("id").asLong();

        MvcResult deleteAdminResult = mockMvc.perform(delete("/plugins/system/admin/users/1")
                        .header(AUTHORIZATION, token))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(json(deleteAdminResult).path("success").asBoolean()).isFalse();

        MvcResult deleteRoleResult = mockMvc.perform(delete("/plugins/system/admin/roles/{id}", roleId)
                        .header(AUTHORIZATION, token))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(json(deleteRoleResult).path("success").asBoolean()).isFalse();

        mockMvc.perform(put("/plugins/system/admin/users/{id}", userId)
                        .header(AUTHORIZATION, token)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "displayName":"审计账号",
                                  "status":"ENABLED",
                                  "roleIds":[]
                                }
                                """))
                .andExpect(status().isOk());

        MvcResult deleteRoleAfterUnbind = mockMvc.perform(delete("/plugins/system/admin/roles/{id}", roleId)
                        .header(AUTHORIZATION, token))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(json(deleteRoleAfterUnbind).path("success").asBoolean()).isTrue();
    }

    @Test
    void shouldSupportRoleSortPermissionsResourcesAndUsers() throws Exception {
        String token = adminToken();

        MvcResult firstRoleResult = mockMvc.perform(post("/plugins/system/admin/roles")
                        .header(AUTHORIZATION, token)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "code":"SORT_ALPHA",
                                  "name":"排序角色A",
                                  "description":"排序测试A",
                                  "sort":50,
                                  "permissionCodes":["role:view"]
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn();
        long firstRoleId = json(firstRoleResult).path("data").path("id").asLong();

        MvcResult secondRoleResult = mockMvc.perform(post("/plugins/system/admin/roles")
                        .header(AUTHORIZATION, token)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "code":"SORT_BETA",
                                  "name":"排序角色B",
                                  "description":"排序测试B",
                                  "sort":5,
                                  "permissionCodes":[]
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn();
        long secondRoleId = json(secondRoleResult).path("data").path("id").asLong();

        MvcResult sortedListResult = mockMvc.perform(get("/plugins/system/admin/roles")
                        .header(AUTHORIZATION, token))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode roles = json(sortedListResult).path("data");
        int betaIndex = findRoleIndex(roles, "SORT_BETA");
        int alphaIndex = findRoleIndex(roles, "SORT_ALPHA");
        assertThat(betaIndex).isGreaterThanOrEqualTo(0);
        assertThat(alphaIndex).isGreaterThanOrEqualTo(0);
        assertThat(betaIndex).isLessThan(alphaIndex);

        mockMvc.perform(put("/plugins/system/admin/roles/{id}", firstRoleId)
                        .header(AUTHORIZATION, token)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "name":"排序角色A-已更新",
                                  "description":"只更新基础信息",
                                  "sort":3
                                }
                                """))
                .andExpect(status().isOk());

        MvcResult permissionsResult = mockMvc.perform(get("/plugins/system/admin/roles/{id}/permissions", firstRoleId)
                        .header(AUTHORIZATION, token))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(json(permissionsResult).path("data")).hasSize(1);
        assertThat(json(permissionsResult).path("data").get(0).asText()).isEqualTo("role:view");

        mockMvc.perform(put("/plugins/system/admin/roles/{id}/permissions", firstRoleId)
                        .header(AUTHORIZATION, token)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                ["role:update","role:view"]
                                """))
                .andExpect(status().isOk());

        MvcResult treeResult = mockMvc.perform(get("/plugins/system/admin/resources/tree")
                        .header(AUTHORIZATION, token))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode systemResources = json(treeResult).path("data").path("systemResources");
        assertThat(systemResources.isArray()).isTrue();
        assertThat(systemResources).isNotEmpty();

        mockMvc.perform(put("/plugins/system/admin/roles/{id}/resources", firstRoleId)
                        .header(AUTHORIZATION, token)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                ["SYSTEM:roles","SYSTEM:roles:grant-permission"]
                                """))
                .andExpect(status().isOk());

        MvcResult roleResourcesResult = mockMvc.perform(get("/plugins/system/admin/roles/{id}/resources", firstRoleId)
                        .header(AUTHORIZATION, token))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(json(roleResourcesResult).path("data")).hasSize(2);

        MvcResult createdUser = mockMvc.perform(post("/plugins/system/admin/users")
                        .header(AUTHORIZATION, token)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "username":"role.binding.user",
                                  "displayName":"角色绑定用户",
                                  "password":"RoleUser@123",
                                  "status":"ENABLED",
                                  "roleIds":[]
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn();
        long userId = json(createdUser).path("data").path("id").asLong();

        mockMvc.perform(put("/plugins/system/admin/roles/{id}/users", firstRoleId)
                        .header(AUTHORIZATION, token)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                [%d]
                                """.formatted(userId)))
                .andExpect(status().isOk());

        MvcResult roleUsersResult = mockMvc.perform(get("/plugins/system/admin/roles/{id}/users", firstRoleId)
                        .header(AUTHORIZATION, token))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(json(roleUsersResult).path("data")).hasSize(1);
        assertThat(json(roleUsersResult).path("data").get(0).path("id").asLong()).isEqualTo(userId);

        MvcResult updatedDetailResult = mockMvc.perform(get("/plugins/system/admin/roles/{id}", firstRoleId)
                        .header(AUTHORIZATION, token))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode detail = json(updatedDetailResult).path("data");
        assertThat(detail.path("sort").asInt()).isEqualTo(3);
        assertThat(detail.path("permissionCount").asInt()).isEqualTo(2);
        assertThat(detail.path("userCount").asInt()).isEqualTo(1);
    }

    private String adminToken() throws Exception {
        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"username":"admin","password":"%s"}
                                """.formatted(CoreSchemaInitializer.DEFAULT_ADMIN_PASSWORD)))
                .andExpect(status().isOk())
                .andReturn();

        return json(loginResult).path("data").path("token").asText();
    }

    private JsonNode json(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private int findRoleIndex(JsonNode roles, String code) {
        for (int index = 0; index < roles.size(); index++) {
            if (code.equals(roles.get(index).path("code").asText())) {
                return index;
            }
        }
        return -1;
    }
}
