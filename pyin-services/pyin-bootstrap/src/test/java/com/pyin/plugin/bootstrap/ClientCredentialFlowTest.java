package com.pyin.plugin.bootstrap;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pyin.plugin.common.constant.PyinHeaders;
import com.pyin.plugin.system.system.CoreSchemaInitializer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:pyin-client-credential-test;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH",
        "spring.datasource.username=sa",
        "spring.datasource.password="
})
class ClientCredentialFlowTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldManageCredentialAuthenticateClientAndRecordLogs() throws Exception {
        String adminToken = adminToken();

        MvcResult createResult = mockMvc.perform(post("/api/core/client-credentials")
                        .header(AUTHORIZATION, adminToken)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"credentialName":"订单系统生产环境"}
                                """))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode created = json(createResult).path("data");
        long credentialId = created.path("id").asLong();
        String accessKey = created.path("accessKey").asText();
        String accessSecret = created.path("accessSecret").asText();
        assertThat(accessKey).startsWith("cck_");
        assertThat(accessSecret).startsWith("ccs_");

        MvcResult listResult = mockMvc.perform(get("/api/core/client-credentials")
                        .header(AUTHORIZATION, adminToken)
                        .param("credentialName", "订单系统"))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode firstCredential = json(listResult).path("data").get(0);
        assertThat(firstCredential.path("id").asLong()).isEqualTo(credentialId);
        assertThat(firstCredential.path("credentialName").asText()).isNotBlank();
        assertThat(firstCredential.has("accessSecret")).isFalse();

        String clientToken = clientToken(accessKey, accessSecret);
        assertThat(clientToken).isNotBlank();

        mockMvc.perform(get("/capi/plugins/dict/client/dict/items")
                        .header(AUTHORIZATION, clientToken))
                .andExpect(status().isNotFound());

        MvcResult rotateResult = mockMvc.perform(post("/api/core/client-credentials/{id}/rotate-secret", credentialId)
                        .header(AUTHORIZATION, adminToken))
                .andExpect(status().isOk())
                .andReturn();
        String rotatedSecret = json(rotateResult).path("data").path("accessSecret").asText();
        assertThat(rotatedSecret).startsWith("ccs_");
        assertThat(rotatedSecret).isNotEqualTo(accessSecret);

        mockMvc.perform(post("/capi/auth/token")
                        .headers(buildClientAuthHeaders(accessKey, accessSecret, "/capi/auth/token", "POST", new byte[0])))
                .andExpect(status().isUnauthorized());

        String rotatedToken = clientToken(accessKey, rotatedSecret);
        assertThat(rotatedToken).isNotBlank();

        mockMvc.perform(post("/api/core/client-credentials/{id}/disable", credentialId)
                        .header(AUTHORIZATION, adminToken))
                .andExpect(status().isOk());

        mockMvc.perform(post("/capi/auth/refresh")
                        .header(AUTHORIZATION, rotatedToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/capi/plugins/dict/client/dict/items")
                        .header(AUTHORIZATION, rotatedToken))
                .andExpect(status().isUnauthorized());

        MvcResult logsResult = mockMvc.perform(get("/api/core/client-credentials/{id}/request-logs", credentialId)
                        .header(AUTHORIZATION, adminToken))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode logs = json(logsResult).path("data");
        assertThat(logs.isArray()).isTrue();
        assertThat(logs).isNotEmpty();
        assertThat(findLog(logs, "AUTH_TOKEN", "SUCCESS")).isTrue();
        assertThat(findLog(logs, "AUTH_TOKEN", "FAILED")).isTrue();
        assertThat(findLog(logs, "PLUGIN_CLIENT_API", "FAILED")).isTrue();
        assertThat(findLog(logs, "AUTH_REFRESH", "FAILED")).isTrue();
    }

    @Test
    void shouldRejectBadTimestampAndRepeatedNonce() throws Exception {
        String adminToken = adminToken();
        MvcResult createResult = mockMvc.perform(post("/api/core/client-credentials")
                        .header(AUTHORIZATION, adminToken)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"credentialName":"报表系统"}
                                """))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode created = json(createResult).path("data");
        String accessKey = created.path("accessKey").asText();
        String accessSecret = created.path("accessSecret").asText();

        byte[] emptyBody = new byte[0];
        String nonce = UUID.randomUUID().toString();
        String expiredTimestamp = String.valueOf(Instant.now().minusSeconds(600).toEpochMilli());
        String bodySha = sha256(emptyBody);

        mockMvc.perform(post("/capi/auth/token")
                        .header(PyinHeaders.ACCESS_KEY, accessKey)
                        .header(PyinHeaders.TIMESTAMP, expiredTimestamp)
                        .header(PyinHeaders.NONCE, nonce)
                        .header(PyinHeaders.BODY_SHA256, bodySha)
                        .header(PyinHeaders.SIGNATURE, signature("POST", "/capi/auth/token", accessKey, expiredTimestamp, nonce, bodySha, accessSecret)))
                .andExpect(status().isRequestTimeout());

        String timestamp = String.valueOf(Instant.now().toEpochMilli());
        String signedNonce = UUID.randomUUID().toString();
        String validSignature = signature("POST", "/capi/auth/token", accessKey, timestamp, signedNonce, bodySha, accessSecret);

        mockMvc.perform(post("/capi/auth/token")
                        .header(PyinHeaders.ACCESS_KEY, accessKey)
                        .header(PyinHeaders.TIMESTAMP, timestamp)
                        .header(PyinHeaders.NONCE, signedNonce)
                        .header(PyinHeaders.BODY_SHA256, bodySha)
                        .header(PyinHeaders.SIGNATURE, validSignature))
                .andExpect(status().isOk());

        mockMvc.perform(post("/capi/auth/token")
                        .header(PyinHeaders.ACCESS_KEY, accessKey)
                        .header(PyinHeaders.TIMESTAMP, timestamp)
                        .header(PyinHeaders.NONCE, signedNonce)
                        .header(PyinHeaders.BODY_SHA256, bodySha)
                        .header(PyinHeaders.SIGNATURE, validSignature))
                .andExpect(status().isConflict());
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

    private String clientToken(String accessKey, String accessSecret) throws Exception {
        MvcResult tokenResult = mockMvc.perform(post("/capi/auth/token")
                        .headers(buildClientAuthHeaders(accessKey, accessSecret, "/capi/auth/token", "POST", new byte[0])))
                .andExpect(status().isOk())
                .andReturn();
        return json(tokenResult).path("data").path("token").asText();
    }

    private org.springframework.http.HttpHeaders buildClientAuthHeaders(
            String accessKey,
            String accessSecret,
            String uri,
            String method,
            byte[] body
    ) throws Exception {
        String timestamp = String.valueOf(Instant.now().toEpochMilli());
        String nonce = UUID.randomUUID().toString();
        String bodySha = sha256(body);
        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
        headers.add(PyinHeaders.ACCESS_KEY, accessKey);
        headers.add(PyinHeaders.TIMESTAMP, timestamp);
        headers.add(PyinHeaders.NONCE, nonce);
        headers.add(PyinHeaders.BODY_SHA256, bodySha);
        headers.add(PyinHeaders.SIGNATURE, signature(method, uri, accessKey, timestamp, nonce, bodySha, accessSecret));
        return headers;
    }

    private String signature(
            String method,
            String uri,
            String accessKey,
            String timestamp,
            String nonce,
            String bodySha,
            String accessSecret
    ) throws Exception {
        String canonical = String.join("\n", method, uri, accessKey, timestamp, nonce, bodySha);
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(accessSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return Base64.getEncoder().encodeToString(mac.doFinal(canonical.getBytes(StandardCharsets.UTF_8)));
    }

    private String sha256(byte[] body) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        return Base64.getEncoder().encodeToString(digest.digest(body == null ? new byte[0] : body));
    }

    private JsonNode json(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private boolean findLog(JsonNode logs, String requestType, String requestStatus) {
        for (JsonNode log : logs) {
            if (requestType.equals(log.path("requestType").asText())
                    && requestStatus.equals(log.path("requestStatus").asText())) {
                return true;
            }
        }
        return false;
    }
}
