package com.pyin.plugin.sdk.manifest;

import static org.assertj.core.api.Assertions.assertThat;

import com.pyin.plugin.sdk.annotation.AdminMapping;
import com.pyin.plugin.sdk.annotation.OpenMapping;
import com.pyin.plugin.sdk.annotation.Permission;
import com.pyin.plugin.spi.PyinPlugin;
import com.pyin.plugin.spi.model.PluginAccessMode;
import com.pyin.plugin.spi.model.PluginManifest;
import com.pyin.plugin.spi.model.PluginPermission;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

class PluginApiScannerTest {

    @Test
    void shouldScanAdminAndOpenControllersWithPluginPrefixes() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(TestConfiguration.class)) {
            PluginApiScanner scanner = new PluginApiScanner();
            TestPlugin plugin = context.getBean(TestPlugin.class);
            PluginScanResult result = scanner.scan(context, plugin, plugin.manifest());

            assertThat(result.apis()).hasSize(4);
            assertThat(result.apis())
                    .anySatisfy(api -> {
                        assertThat(api.path()).isEqualTo("/types");
                        assertThat(api.internalPath()).isEqualTo("/plugins/test/admin/types");
                        assertThat(api.method()).isEqualTo("GET");
                        assertThat(api.accessMode()).isEqualTo(PluginAccessMode.CENTER_ADMIN_ONLY);
                        assertThat(api.permissionCode()).isEqualTo("test:view");
                    })
                    .anySatisfy(api -> {
                        assertThat(api.path()).isEqualTo("/types/{id}");
                        assertThat(api.internalPath()).isEqualTo("/plugins/test/admin/types/{id}");
                        assertThat(api.method()).isEqualTo("DELETE");
                        assertThat(api.permissionCode()).isEqualTo("test:admin:types:id:delete");
                    })
                    .anySatisfy(api -> {
                        assertThat(api.path()).isEqualTo("/batch");
                        assertThat(api.internalPath()).isEqualTo("/plugins/test/open/batch");
                        assertThat(api.method()).isEqualTo("POST");
                        assertThat(api.accessMode()).isEqualTo(PluginAccessMode.CLIENT_SDK_GATEWAY);
                        assertThat(api.permissionCode()).isEqualTo("test:open:batch:post");
                    });

            assertThat(result.permissions())
                    .extracting(PluginPermission::code)
                    .containsExactlyInAnyOrder(
                            "test:view",
                            "test:admin:types:id:delete",
                            "test:open:items:get",
                            "test:open:batch:post"
                    );
        }
    }

    @Configuration
    static class TestConfiguration {

        @Bean
        TestPlugin testPlugin() {
            return new TestPlugin();
        }

        @Bean
        TestAdminController testAdminController() {
            return new TestAdminController();
        }

        @Bean
        TestOpenController testOpenController() {
            return new TestOpenController();
        }
    }

    static class TestPlugin implements PyinPlugin {

        @Override
        public PluginManifest manifest() {
            return PluginManifest.builder("test").pluginName("test").build();
        }

    }

    @AdminMapping
    static class TestAdminController {

        @Permission(code = "test:view", name = "查看测试")
        @GetMapping("/types")
        public String listTypes() {
            return "ok";
        }

        @DeleteMapping("/types/{id}")
        public String deleteType(@PathVariable String id) {
            return id;
        }
    }

    @OpenMapping
    static class TestOpenController {

        @GetMapping("/items")
        public String items() {
            return "ok";
        }

        @PostMapping("/batch")
        public String batch() {
            return "ok";
        }
    }
}
