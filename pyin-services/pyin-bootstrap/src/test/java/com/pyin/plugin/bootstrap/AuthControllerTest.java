package com.pyin.plugin.bootstrap;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pyin.plugin.system.system.CoreSchemaInitializer;
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
        "spring.datasource.url=jdbc:h2:mem:pyin-auth-test;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH",
        "spring.datasource.username=sa",
        "spring.datasource.password="
})
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldLoginWithDefaultAdminCredentials() throws Exception {
        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"username":"admin","password":"%s"}
                                """.formatted(CoreSchemaInitializer.DEFAULT_ADMIN_PASSWORD)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode loginJson = json(loginResult);
        String token = loginJson.path("data").path("token").asText();

        assertThat(token).isNotBlank();

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

    private JsonNode json(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }
}
