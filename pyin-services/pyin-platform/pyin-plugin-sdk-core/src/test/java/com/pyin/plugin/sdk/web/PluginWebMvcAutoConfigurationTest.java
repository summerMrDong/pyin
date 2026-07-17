package com.pyin.plugin.sdk.web;

import static org.assertj.core.api.Assertions.assertThat;

import com.pyin.plugin.sdk.annotation.AdminMapping;
import com.pyin.plugin.sdk.annotation.OpenMapping;
import com.pyin.plugin.spi.PyinPlugin;
import com.pyin.plugin.spi.model.PluginManifest;
import java.lang.reflect.Method;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;

class PluginWebMvcAutoConfigurationTest {

    @Test
    void shouldRegisterPluginAwareAdminAndOpenPrefixes() throws NoSuchMethodException {
        PluginOwnershipResolver ownershipResolver = new PluginOwnershipResolver(List.of(new DemoPlugin()));
        PluginRequestMappingHandlerMapping mapping = new PluginRequestMappingHandlerMapping(ownershipResolver);

        Method adminMethod = DemoAdminController.class.getDeclaredMethod("ping");
        RequestMappingInfo adminMapping = ReflectionTestUtils.invokeMethod(
                mapping,
                "getMappingForMethod",
                adminMethod,
                DemoAdminController.class
        );
        assertThat(adminMapping).isNotNull();
        assertThat(adminMapping.getPatternValues()).containsExactly("/demo/admin/ping");

        Method openMethod = DemoOpenController.class.getDeclaredMethod("pong");
        RequestMappingInfo openMapping = ReflectionTestUtils.invokeMethod(
                mapping,
                "getMappingForMethod",
                openMethod,
                DemoOpenController.class
        );
        assertThat(openMapping).isNotNull();
        assertThat(openMapping.getPatternValues()).containsExactly("/demo/open/pong");
    }

    static class DemoPlugin implements PyinPlugin {

        @Override
        public String pluginId() {
            return "demo";
        }

        @Override
        public PluginManifest manifest() {
            return PluginManifest.builder().pluginId("demo").pluginName("demo").build();
        }

        @Override
        public List<com.pyin.plugin.spi.model.PluginMenu> menus() {
            return List.of();
        }
    }

    @AdminMapping
    static class DemoAdminController {

        @GetMapping("/ping")
        public String ping() {
            return "admin";
        }
    }

    @OpenMapping
    static class DemoOpenController {

        @GetMapping("/pong")
        public String pong() {
            return "open";
        }
    }
}
