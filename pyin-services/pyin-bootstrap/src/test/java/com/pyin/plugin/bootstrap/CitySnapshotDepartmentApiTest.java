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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:citysnapshot-department;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.sql.init.schema-locations=classpath:city-snapshot-test-schema.sql",
        "spring.sql.init.mode=always"
})
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class CitySnapshotDepartmentApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private static Long issueId;

    @Test
    @Order(1)
    void shouldPrepareTaskAndQueryApis() throws Exception {
        jdbcTemplate.update("""
                insert into t_issue (
                    issue_no, citizen_user_id, issue_type_id, region_id, issue_title, issue_desc, location_text,
                    current_status, current_department_id, evaluation_status, overdue_flag, report_at, created_at, updated_at, deleted
                ) values ('DEPT-ISSUE-001', 1001, 1, 1, '部门处理事项', '部门待处理事项', '测试位置', 'PENDING_PROCESS', 1, 'PENDING', 0,
                    current_timestamp, current_timestamp, current_timestamp, 0)
                """);
        issueId = jdbcTemplate.queryForObject("select id from t_issue where issue_no = 'DEPT-ISSUE-001'", Long.class);

        mockMvc.perform(get("/department/dashboard/overview").requestAttr(INTERNAL_REQUEST_ATTRIBUTE, true)
                        .param("departmentId", "1"))
                .andExpect(status().isOk())
                .andExpect(header().string(HEADER_API_ID, "API-027"))
                .andExpect(header().string(HEADER_MODULE_ID, "MOD-003"))
                .andExpect(header().string(HEADER_FUNCTION_POINT_ID, "FP-027"))
                .andExpect(header().string(HEADER_PLUGIN_ID, "city-snapshot-department"));

        mockMvc.perform(get("/department/tasks").requestAttr(INTERNAL_REQUEST_ATTRIBUTE, true)
                        .param("departmentId", "1"))
                .andExpect(status().isOk())
                .andExpect(header().string(HEADER_API_ID, "API-028"));
    }

    @Test
    @Order(2)
    void shouldStartAndCompleteTask() throws Exception {
        mockMvc.perform(patch("/department/tasks/{issueId}/start", issueId)
                        .requestAttr(INTERNAL_REQUEST_ATTRIBUTE, true)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"operatorUserId":3001}
                                """))
                .andExpect(status().isOk())
                .andExpect(header().string(HEADER_API_ID, "API-029"));

        mockMvc.perform(post("/department/tasks/{issueId}/complete", issueId)
                        .requestAttr(INTERNAL_REQUEST_ATTRIBUTE, true)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"operatorUserId":3001,"resultDesc":"已清理完毕"}
                                """))
                .andExpect(status().isOk())
                .andExpect(header().string(HEADER_API_ID, "API-030"));

        mockMvc.perform(get("/department/tasks/{issueId}/history-detail", issueId)
                        .requestAttr(INTERNAL_REQUEST_ATTRIBUTE, true)
                        .param("departmentId", "1"))
                .andExpect(status().isOk())
                .andExpect(header().string(HEADER_API_ID, "API-032"));
    }

    @Test
    @Order(3)
    void shouldReturnTask() throws Exception {
        jdbcTemplate.update("update t_issue set current_status = 'PENDING_PROCESS', current_department_id = 1 where id = ?", issueId);
        mockMvc.perform(patch("/department/tasks/{issueId}/return", issueId)
                        .requestAttr(INTERNAL_REQUEST_ATTRIBUTE, true)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"operatorUserId":3001,"returnReason":"非本部门职责"}
                                """))
                .andExpect(status().isOk())
                .andExpect(header().string(HEADER_API_ID, "API-031"));
    }
}
