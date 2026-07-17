package com.pyin.plugin.system.security;

import com.pyin.plugin.common.security.PluginAdminAccessDecision;
import com.pyin.plugin.common.security.PluginAdminAccessService;
import com.pyin.plugin.system.plugin.CompiledApiRule;
import com.pyin.plugin.system.plugin.CompiledPluginApiRegistry;
import com.pyin.plugin.spi.model.PluginAccessMode;
import org.springframework.stereotype.Service;

@Service
public class AccessControlServiceImpl implements PluginAdminAccessService {

    private final CompiledPluginApiRegistry compiledPluginApiRegistry;

    public AccessControlServiceImpl(
            CompiledPluginApiRegistry compiledPluginApiRegistry
    ) {
        this.compiledPluginApiRegistry = compiledPluginApiRegistry;
    }

    @Override
    public PluginAdminAccessDecision checkAccess(String pluginId, String method, String path) {
        CompiledApiRule apiRule = compiledPluginApiRegistry.match(
                        pluginId,
                        method,
                        path,
                        PluginAccessMode.CENTER_ADMIN_ONLY
                )
                .orElse(null);
        if (apiRule == null) {
            return PluginAdminAccessDecision.deny(
                    "PYIN-PLUGIN-403",
                    "Plugin api is not published for admin gateway: " + pluginId + " " + method + " " + path
            );
        }
        return PluginAdminAccessDecision.allow(apiRule.permissionCode());
    }
}
