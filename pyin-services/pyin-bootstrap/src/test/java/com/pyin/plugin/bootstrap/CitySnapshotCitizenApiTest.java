package com.pyin.plugin.bootstrap;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static com.pyin.gateway.support.PluginGatewayInternalSupport.INTERNAL_REQUEST_ATTRIBUTE;
import static com.pyin.plugin.sdk.web.ApiSpecMetadataResponseAdvice.HEADER_API_ID;
import static com.pyin.plugin.sdk.web.ApiSpecMetadataResponseAdvice.HEADER_FUNCTION_POINT_ID;
import static com.pyin.plugin.sdk.web.ApiSpecMetadataResponseAdvice.HEADER_MODULE_ID;
import static com.pyin.plugin.sdk.web.ApiSpecMetadataResponseAdvice.HEADER_PLUGIN_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:citysnapshot-citizen;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.sql.init.schema-locations=classpath:city-snapshot-test-schema.sql",
        "spring.sql.init.mode=always"
})
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class CitySnapshotCitizenApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private static Long citizenUserId;
    private static Long attachmentId;
    private static Long issueId;

    @Test
    @Order(1)
    void shouldRegisterCitizen() throws Exception {
        MvcResult result = mockMvc.perform(post("/citizen/auth/register")
                        .requestAttr(INTERNAL_REQUEST_ATTRIBUTE, true)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"phone":"13800000009","password":"123456","nickname":"测试市民"}
                                """))
                .andExpect(status().isOk())
                .andExpect(header().string(HEADER_API_ID, "API-001"))
                .andExpect(header().string(HEADER_MODULE_ID, "MOD-001"))
                .andExpect(header().string(HEADER_FUNCTION_POINT_ID, "FP-001"))
                .andExpect(header().string(HEADER_PLUGIN_ID, "city-snapshot-citizen"))
                .andReturn();
        JsonNode json = json(result);
        citizenUserId = json.path("data").path("userId").asLong();
        assertThat(citizenUserId).isGreaterThan(0);
    }

    @Test
    @Order(2)
    void shouldLoginAndUpdatePassword() throws Exception {
        mockMvc.perform(post("/citizen/auth/login")
                        .requestAttr(INTERNAL_REQUEST_ATTRIBUTE, true)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"phone":"13800000009","password":"123456"}
                                """))
                .andExpect(status().isOk())
                .andExpect(header().string(HEADER_API_ID, "API-002"));

        mockMvc.perform(put("/citizen/auth/password")
                        .requestAttr(INTERNAL_REQUEST_ATTRIBUTE, true)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"userId":%d,"oldPassword":"123456","newPassword":"1234567"}
                                """.formatted(citizenUserId)))
                .andExpect(status().isOk())
                .andExpect(header().string(HEADER_API_ID, "API-003"));
    }

    @Test
    @Order(3)
    void shouldUploadAttachmentAndCreateIssue() throws Exception {
        MockMultipartFile imageFile = new MockMultipartFile("file", "test.jpg", "image/jpeg", "img".getBytes());
        MvcResult uploadResult = mockMvc.perform(multipart("/citizen/issues/attachments")
                        .file(imageFile)
                        .requestAttr(INTERNAL_REQUEST_ATTRIBUTE, true)
                        .param("userId", String.valueOf(citizenUserId)))
                .andExpect(status().isOk())
                .andExpect(header().string(HEADER_API_ID, "API-004"))
                .andReturn();
        attachmentId = json(uploadResult).path("data").path("attachmentId").asLong();

        mockMvc.perform(get("/citizen/issues/report-options")
                        .requestAttr(INTERNAL_REQUEST_ATTRIBUTE, true))
                .andExpect(status().isOk())
                .andExpect(header().string(HEADER_API_ID, "API-005"));

        MvcResult createResult = mockMvc.perform(post("/citizen/issues")
                        .requestAttr(INTERNAL_REQUEST_ATTRIBUTE, true)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "citizenUserId": %d,
                                  "issueTypeId": 1,
                                  "regionId": 1,
                                  "issueTitle": "道路边存在垃圾堆积",
                                  "issueDesc": "需要尽快清理",
                                  "locationText": "松北街道测试路口",
                                  "attachmentIds": [%d]
                                }
                                """.formatted(citizenUserId, attachmentId)))
                .andExpect(status().isOk())
                .andExpect(header().string(HEADER_API_ID, "API-006"))
                .andReturn();
        issueId = json(createResult).path("data").path("issueId").asLong();
        assertThat(issueId).isGreaterThan(0);
    }

    @Test
    @Order(4)
    void shouldQueryCitizenViews() throws Exception {
        mockMvc.perform(get("/citizen/issues/my")
                        .requestAttr(INTERNAL_REQUEST_ATTRIBUTE, true)
                        .param("citizenUserId", String.valueOf(citizenUserId)))
                .andExpect(status().isOk())
                .andExpect(header().string(HEADER_API_ID, "API-007"));

        mockMvc.perform(get("/citizen/issues/my/filter")
                        .requestAttr(INTERNAL_REQUEST_ATTRIBUTE, true)
                        .param("citizenUserId", String.valueOf(citizenUserId))
                        .param("status", "PENDING_DISPATCH"))
                .andExpect(status().isOk())
                .andExpect(header().string(HEADER_API_ID, "API-008"));

        mockMvc.perform(get("/citizen/issues/{issueId}", issueId)
                        .requestAttr(INTERNAL_REQUEST_ATTRIBUTE, true)
                        .param("citizenUserId", String.valueOf(citizenUserId)))
                .andExpect(status().isOk())
                .andExpect(header().string(HEADER_API_ID, "API-009"));

        mockMvc.perform(get("/citizen/profile")
                        .requestAttr(INTERNAL_REQUEST_ATTRIBUTE, true)
                        .param("userId", String.valueOf(citizenUserId)))
                .andExpect(status().isOk())
                .andExpect(header().string(HEADER_API_ID, "API-011"));
    }

    @Test
    @Order(5)
    void shouldEvaluateAndQueryNotices() throws Exception {
        jdbcTemplate.update("update t_issue set current_status = 'COMPLETED', completed_at = current_timestamp where id = ?", issueId);

        mockMvc.perform(post("/citizen/issues/{issueId}/evaluations", issueId)
                        .requestAttr(INTERNAL_REQUEST_ATTRIBUTE, true)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"citizenUserId":%d,"ratingLevel":"SATISFIED","ratingScore":5,"ratingRemark":"处理及时"}
                                """.formatted(citizenUserId)))
                .andExpect(status().isOk())
                .andExpect(header().string(HEADER_API_ID, "API-010"));

        mockMvc.perform(get("/citizen/evaluations")
                        .requestAttr(INTERNAL_REQUEST_ATTRIBUTE, true)
                        .param("citizenUserId", String.valueOf(citizenUserId)))
                .andExpect(status().isOk())
                .andExpect(header().string(HEADER_API_ID, "API-012"));

        mockMvc.perform(get("/citizen/notices")
                        .requestAttr(INTERNAL_REQUEST_ATTRIBUTE, true)
                        .param("targetUserId", String.valueOf(citizenUserId)))
                .andExpect(status().isOk())
                .andExpect(header().string(HEADER_API_ID, "API-013"));
    }

    private JsonNode json(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }
}
