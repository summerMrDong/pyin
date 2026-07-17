package com.pyin.plugin.bootstrap;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static com.pyin.gateway.support.PluginGatewayInternalSupport.INTERNAL_REQUEST_ATTRIBUTE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:citysnapshot-flow;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.sql.init.schema-locations=classpath:city-snapshot-test-schema.sql",
        "spring.sql.init.mode=always"
})
class CitySnapshotFlowTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void shouldRunMainFlowClosedLoop() throws Exception {
        Long citizenUserId = registerCitizen();
        Long issueId = createIssue(citizenUserId);

        mockMvc.perform(patch("/issues/{issueId}/dispatch", issueId)
                        .requestAttr(INTERNAL_REQUEST_ATTRIBUTE, true)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"departmentId":1,"assignedUserId":3001,"dispatchRemark":"主流程派发","operatorUserId":2001}
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(patch("/department/tasks/{issueId}/start", issueId)
                        .requestAttr(INTERNAL_REQUEST_ATTRIBUTE, true)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"operatorUserId":3001}
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(post("/department/tasks/{issueId}/complete", issueId)
                        .requestAttr(INTERNAL_REQUEST_ATTRIBUTE, true)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"operatorUserId":3001,"resultDesc":"已完成处理"}
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(post("/citizen/issues/{issueId}/evaluations", issueId)
                        .requestAttr(INTERNAL_REQUEST_ATTRIBUTE, true)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"citizenUserId":%d,"ratingLevel":"SATISFIED","ratingScore":5,"ratingRemark":"很好"}
                                """.formatted(citizenUserId)))
                .andExpect(status().isOk());

        String issueStatus = jdbcTemplate.queryForObject("select current_status from t_issue where id = ?", String.class, issueId);
        Integer flowCount = jdbcTemplate.queryForObject("select count(1) from t_issue_flow_log where issue_id = ?", Integer.class, issueId);

        assertThat(issueStatus).isEqualTo("EVALUATED");
        assertThat(flowCount).isGreaterThanOrEqualTo(4);
    }

    @Test
    void shouldRunRejectFlow() throws Exception {
        Long citizenUserId = registerCitizen();
        Long issueId = createIssue(citizenUserId);

        mockMvc.perform(patch("/issues/{issueId}/reject", issueId)
                        .requestAttr(INTERNAL_REQUEST_ATTRIBUTE, true)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"rejectType":"INVALID","rejectReason":"信息不完整","operatorUserId":2001}
                                """))
                .andExpect(status().isOk());

        String issueStatus = jdbcTemplate.queryForObject("select current_status from t_issue where id = ?", String.class, issueId);
        assertThat(issueStatus).isEqualTo("REJECTED");
    }

    @Test
    void shouldRunReturnAndRedispatchFlow() throws Exception {
        Long citizenUserId = registerCitizen();
        Long issueId = createIssue(citizenUserId);

        mockMvc.perform(patch("/issues/{issueId}/dispatch", issueId)
                        .requestAttr(INTERNAL_REQUEST_ATTRIBUTE, true)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"departmentId":1,"assignedUserId":3001,"dispatchRemark":"派发","operatorUserId":2001}
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(patch("/department/tasks/{issueId}/return", issueId)
                        .requestAttr(INTERNAL_REQUEST_ATTRIBUTE, true)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"operatorUserId":3001,"returnReason":"需要平台重新指派"}
                                """))
                .andExpect(status().isOk());

        String returnedStatus = jdbcTemplate.queryForObject("select current_status from t_issue where id = ?", String.class, issueId);
        assertThat(returnedStatus).isEqualTo("RETURNED");

        mockMvc.perform(patch("/issues/{issueId}/redispatch", issueId)
                        .requestAttr(INTERNAL_REQUEST_ATTRIBUTE, true)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"newDepartmentId":2,"newAssignedUserId":3002,"redispatchRemark":"异常回路重派","operatorUserId":2001}
                                """))
                .andExpect(status().isOk());

        String redispatchedStatus = jdbcTemplate.queryForObject("select current_status from t_issue where id = ?", String.class, issueId);
        assertThat(redispatchedStatus).isEqualTo("PENDING_PROCESS");
    }

    @Test
    void shouldQueryStatistics() throws Exception {
        mockMvc.perform(get("/system/statistics/report-status").requestAttr(INTERNAL_REQUEST_ATTRIBUTE, true))
                .andExpect(status().isOk());
    }

    private Long registerCitizen() throws Exception {
        MvcResult result = mockMvc.perform(post("/citizen/auth/register")
                        .requestAttr(INTERNAL_REQUEST_ATTRIBUTE, true)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"phone":"138000000%s","password":"123456","nickname":"闭环市民"}
                                """.formatted(System.currentTimeMillis() % 10000)))
                .andExpect(status().isOk())
                .andReturn();
        return json(result).path("data").path("userId").asLong();
    }

    private Long createIssue(Long citizenUserId) throws Exception {
        MvcResult result = mockMvc.perform(post("/citizen/issues")
                        .requestAttr(INTERNAL_REQUEST_ATTRIBUTE, true)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "citizenUserId": %d,
                                  "issueTypeId": 1,
                                  "regionId": 1,
                                  "issueTitle": "闭环测试事项",
                                  "issueDesc": "闭环测试描述",
                                  "locationText": "闭环测试位置"
                                }
                                """.formatted(citizenUserId)))
                .andExpect(status().isOk())
                .andReturn();
        return json(result).path("data").path("issueId").asLong();
    }

    private JsonNode json(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }
}
