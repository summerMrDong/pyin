package com.pyin.plugin.bootstrap;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static com.pyin.gateway.support.PluginGatewayInternalSupport.INTERNAL_REQUEST_ATTRIBUTE;
import static com.pyin.plugin.sdk.web.ApiSpecMetadataResponseAdvice.HEADER_API_ID;
import static com.pyin.plugin.sdk.web.ApiSpecMetadataResponseAdvice.HEADER_FUNCTION_POINT_ID;
import static com.pyin.plugin.sdk.web.ApiSpecMetadataResponseAdvice.HEADER_MODULE_ID;
import static com.pyin.plugin.sdk.web.ApiSpecMetadataResponseAdvice.HEADER_PLUGIN_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:citysnapshot-admin;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.sql.init.schema-locations=classpath:city-snapshot-test-schema.sql",
        "spring.sql.init.mode=always"
})
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class CitySnapshotAdminApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private static Long issueId;

    @Test
    @Order(1)
    void shouldPrepareIssueAndQueryDashboard() throws Exception {
        jdbcTemplate.update("""
                insert into t_issue (
                    issue_no, citizen_user_id, issue_type_id, region_id, issue_title, issue_desc, location_text,
                    current_status, evaluation_status, overdue_flag, report_at, created_at, updated_at, deleted
                ) values ('ADMIN-ISSUE-001', 1001, 1, 1, '测试事项', '待平台派发事项', '松北街道', 'PENDING_DISPATCH', 'PENDING', 0,
                    current_timestamp, current_timestamp, current_timestamp, 0)
                """);
        issueId = jdbcTemplate.queryForObject("select id from t_issue where issue_no = 'ADMIN-ISSUE-001'", Long.class);

        mockMvc.perform(get("/dashboard/overview").requestAttr(INTERNAL_REQUEST_ATTRIBUTE, true))
                .andExpect(status().isOk())
                .andExpect(header().string(HEADER_API_ID, "API-014"))
                .andExpect(header().string(HEADER_MODULE_ID, "MOD-006"))
                .andExpect(header().string(HEADER_FUNCTION_POINT_ID, "FP-014"))
                .andExpect(header().string(HEADER_PLUGIN_ID, "city-snapshot-admin"));

        mockMvc.perform(get("/dashboard/latest-issues").requestAttr(INTERNAL_REQUEST_ATTRIBUTE, true))
                .andExpect(status().isOk())
                .andExpect(header().string(HEADER_API_ID, "API-015"));

        mockMvc.perform(get("/dashboard/supervision").requestAttr(INTERNAL_REQUEST_ATTRIBUTE, true))
                .andExpect(status().isOk())
                .andExpect(header().string(HEADER_API_ID, "API-016"));
    }

    @Test
    @Order(2)
    void shouldDispatchAndQueryResultApis() throws Exception {
        mockMvc.perform(get("/issues/{issueId}/pending-detail", issueId).requestAttr(INTERNAL_REQUEST_ATTRIBUTE, true))
                .andExpect(status().isOk())
                .andExpect(header().string(HEADER_API_ID, "API-017"));

        mockMvc.perform(put("/issues/{issueId}/basic-info", issueId)
                        .requestAttr(INTERNAL_REQUEST_ATTRIBUTE, true)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"issueTitle":"修改后的标题","issueDesc":"修改后的描述","operatorUserId":2001}
                                """))
                .andExpect(status().isOk())
                .andExpect(header().string(HEADER_API_ID, "API-018"));

        mockMvc.perform(patch("/issues/{issueId}/dispatch", issueId)
                        .requestAttr(INTERNAL_REQUEST_ATTRIBUTE, true)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"departmentId":1,"assignedUserId":3001,"dispatchRemark":"请尽快处理","operatorUserId":2001}
                                """))
                .andExpect(status().isOk())
                .andExpect(header().string(HEADER_API_ID, "API-019"));

        jdbcTemplate.update("update t_issue set current_status = 'COMPLETED', completed_at = current_timestamp where id = ?", issueId);
        mockMvc.perform(get("/issues/{issueId}/result", issueId).requestAttr(INTERNAL_REQUEST_ATTRIBUTE, true))
                .andExpect(status().isOk())
                .andExpect(header().string(HEADER_API_ID, "API-022"));
    }

    @Test
    @Order(3)
    void shouldRejectAndRedispatch() throws Exception {
        jdbcTemplate.update("update t_issue set current_status = 'PENDING_DISPATCH', current_department_id = null where id = ?", issueId);
        mockMvc.perform(patch("/issues/{issueId}/reject", issueId)
                        .requestAttr(INTERNAL_REQUEST_ATTRIBUTE, true)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"rejectType":"INVALID","rejectReason":"信息不完整","operatorUserId":2001}
                                """))
                .andExpect(status().isOk())
                .andExpect(header().string(HEADER_API_ID, "API-020"));

        jdbcTemplate.update("update t_issue set current_status = 'RETURNED', current_department_id = 2 where id = ?", issueId);
        mockMvc.perform(patch("/issues/{issueId}/redispatch", issueId)
                        .requestAttr(INTERNAL_REQUEST_ATTRIBUTE, true)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"newDepartmentId":1,"newAssignedUserId":3001,"redispatchRemark":"重新派发","operatorUserId":2001}
                                """))
                .andExpect(status().isOk())
                .andExpect(header().string(HEADER_API_ID, "API-021"));
    }

    @Test
    @Order(4)
    void shouldQueryStatisticsAndSystemApis() throws Exception {
        mockMvc.perform(get("/system/statistics/report-status").requestAttr(INTERNAL_REQUEST_ATTRIBUTE, true))
                .andExpect(status().isOk())
                .andExpect(header().string(HEADER_API_ID, "API-023"));
        mockMvc.perform(get("/system/statistics/type-region-department").requestAttr(INTERNAL_REQUEST_ATTRIBUTE, true))
                .andExpect(status().isOk())
                .andExpect(header().string(HEADER_API_ID, "API-024"));

        mockMvc.perform(post("/system/base-configs")
                        .requestAttr(INTERNAL_REQUEST_ATTRIBUTE, true)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"configGroup":"ISSUE_TYPE","configKey":"ORDER_TEST","configValue":"秩序问题","description":"测试配置"}
                                """))
                .andExpect(status().isOk())
                .andExpect(header().string(HEADER_API_ID, "API-025"));

        mockMvc.perform(put("/system/users/{userId}/roles", 3001)
                        .requestAttr(INTERNAL_REQUEST_ATTRIBUTE, true)
                        .param("userId", "3001")
                        .param("roleCodes", "DEPARTMENT"))
                .andExpect(status().isOk())
                .andExpect(header().string(HEADER_API_ID, "API-026"));
    }
}
