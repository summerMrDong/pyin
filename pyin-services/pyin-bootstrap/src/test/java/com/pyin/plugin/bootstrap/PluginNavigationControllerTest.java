package com.pyin.plugin.bootstrap;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pyin.plugin.system.system.CoreSchemaInitializer;
import com.pyin.plugin.spi.PyinPlugin;
import com.pyin.plugin.spi.model.PluginManifest;
import com.pyin.plugin.spi.model.PluginMenu;
import com.pyin.plugin.spi.model.PluginMenuOpenMode;
import com.pyin.plugin.spi.model.PluginMenuType;
import com.pyin.plugin.spi.model.PluginRuntimeMode;
import com.pyin.plugin.spi.model.PluginType;
import java.util.List;
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
        "spring.datasource.password="
})
class PluginNavigationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldReturnUnifiedMenusForSystemAndPluginNavigation() throws Exception {
        String token = login();

        MvcResult navigationResult = mockMvc.perform(get("/api/core/plugins/navigation")
                        .header(AUTHORIZATION, token))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode plugins = json(navigationResult).path("data");
        assertThat(plugins.isArray()).isTrue();
        assertThat(findPlugin(plugins, "pyin-system")).isNotNull();
        assertThat(findPlugin(plugins, "dict")).isNotNull();
        assertThat(findPlugin(plugins, "test-navigation")).isNotNull();

        JsonNode systemPlugin = findPlugin(plugins, "pyin-system");
        assertThat(systemPlugin.has("menus")).isTrue();
        assertThat(systemPlugin.has("routes")).isFalse();
        assertThat(hasMenuPath(systemPlugin.path("menus"), "/users")).isTrue();

        JsonNode testPlugin = findPlugin(plugins, "test-navigation");
        assertThat(testPlugin.path("menus").size()).isEqualTo(1);

        JsonNode directoryMenu = testPlugin.path("menus").get(0);
        assertThat(directoryMenu.path("type").asText()).isEqualTo("DIRECTORY");
        assertThat(directoryMenu.path("children").size()).isEqualTo(2);

        JsonNode routeMenu = directoryMenu.path("children").get(0);
        assertThat(routeMenu.path("type").asText()).isEqualTo("ROUTE");
        assertThat(routeMenu.path("path").asText()).isEqualTo("/plugins/test-navigation/home");

        JsonNode linkMenu = directoryMenu.path("children").get(1);
        assertThat(linkMenu.path("type").asText()).isEqualTo("LINK");
        assertThat(linkMenu.path("url").asText()).isEqualTo("https://example.com/docs");
        assertThat(linkMenu.path("openMode").asText()).isEqualTo("IFRAME");
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

    private boolean hasMenuPath(JsonNode menus, String path) {
        for (JsonNode menu : menus) {
            if (path.equals(menu.path("path").asText())) {
                return true;
            }
        }
        return false;
    }

    @TestConfiguration
    static class PluginNavigationTestConfiguration {

        @Bean
        PyinPlugin navigationFixturePlugin() {
            return new PyinPlugin() {
                @Override
                public String pluginId() {
                    return "test-navigation";
                }

                @Override
                public PluginManifest manifest() {
                    return PluginManifest.builder()
                            .pluginId("test-navigation")
                            .pluginName("导航测试插件")
                            .pluginVersion("1.0.0")
                            .pluginType(PluginType.EXTERNAL)
                            .runtimeMode(PluginRuntimeMode.EMBEDDED)
                            .basePath("/plugins/test-navigation")
                            .build();
                }

                @Override
                public List<PluginMenu> menus() {
                    return List.of(
                            new PluginMenu(
                                    "test-navigation:group",
                                    "测试分组",
                                    PluginMenuType.DIRECTORY,
                                    null,
                                    null,
                                    "Folder",
                                    100,
                                    null,
                                    null,
                                    null,
                                    List.of(
                                            new PluginMenu(
                                                    "test-navigation:home",
                                                    "测试首页",
                                                    PluginMenuType.ROUTE,
                                                    "/plugins/test-navigation/home",
                                                    null,
                                                    "House",
                                                    10,
                                                    null,
                                                    null,
                                                    null,
                                                    List.of()
                                            ),
                                            new PluginMenu(
                                                    "test-navigation:docs",
                                                    "帮助文档",
                                                    PluginMenuType.LINK,
                                                    null,
                                                    "https://example.com/docs",
                                                    "Link",
                                                    20,
                                                    null,
                                                    PluginMenuOpenMode.IFRAME,
                                                    null,
                                                    List.of()
                                            )
                                    )
                            )
                    );
                }
            };
        }
    }
}
