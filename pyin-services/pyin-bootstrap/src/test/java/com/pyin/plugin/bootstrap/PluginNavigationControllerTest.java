package com.pyin.plugin.bootstrap;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pyin.plugin.system.setting.support.CoreSchemaInitializer;
import com.pyin.plugin.spi.PyinPlugin;
import com.pyin.plugin.spi.model.PluginManifest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
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
@Import(PluginNavigationControllerTest.PluginNavigationTestConfiguration.class)
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:pyin-plugin-navigation-test;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "pyin.center.plugin-runtime.embedded-plugin-ids=system,config,dict,test-navigation"
})
class PluginNavigationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldReturnStartedPluginWorkspacesWithoutMenus() throws Exception {
        String token = login();

        MvcResult navigationResult = mockMvc.perform(get("/plugins/system/admin/plugins/workspaces")
                        .header(AUTHORIZATION, token))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode plugins = json(navigationResult).path("data");
        assertThat(plugins.isArray()).isTrue();
        assertThat(findPlugin(plugins, "system")).isNotNull();
        assertThat(findPlugin(plugins, "dict")).isNotNull();
        assertThat(findPlugin(plugins, "test-navigation")).isNotNull();

        JsonNode testPlugin = findPlugin(plugins, "test-navigation");
        assertThat(testPlugin.has("menus")).isFalse();
        assertThat(testPlugin.path("frontend").has("remoteName")).isFalse();
        assertThat(testPlugin.path("frontend").path("remoteEntry").asText())
                .isEqualTo("/plugin-static/test-navigation/assets/remoteEntry.js");
    }

    @Test
    void shouldNotExposeLegacySystemPluginRoute() throws Exception {
        mockMvc.perform(get("/plugins/pyin-system/admin/plugins/workspaces")
                        .header(AUTHORIZATION, login()))
                .andExpect(status().isNotFound());
    }

    private String login() throws Exception {
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

    private JsonNode findPlugin(JsonNode plugins, String pluginId) {
        for (JsonNode plugin : plugins) {
            if (pluginId.equals(plugin.path("pluginId").asText())) {
                return plugin;
            }
        }
        return null;
    }

    @TestConfiguration
    static class PluginNavigationTestConfiguration {

        @Bean
        PyinPlugin navigationFixturePlugin() {
            return new PyinPlugin() {
                @Override
                public PluginManifest manifest() {
                    return PluginManifest.builder("test-navigation")
                            .pluginName("导航测试插件")
                            .pluginVersion("1.0.0")
                            .basePath("/plugins/test-navigation")
                            .build();
                }

            };
        }
    }
}
