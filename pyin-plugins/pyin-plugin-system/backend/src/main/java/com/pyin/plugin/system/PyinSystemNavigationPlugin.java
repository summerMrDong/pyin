package com.pyin.plugin.system;

import com.pyin.plugin.sdk.annotation.PluginComponent;
import com.pyin.plugin.spi.PyinPlugin;
import com.pyin.plugin.spi.model.*;

import java.util.List;

@PluginComponent
public class PyinSystemNavigationPlugin implements PyinPlugin {

    public static final String PLUGIN_ID = "pyin-system";

    @Override
    public String pluginId() {
        return PLUGIN_ID;
    }

    @Override
    public PluginManifest manifest() {
        return PluginManifest.builder()
                .pluginId(PLUGIN_ID)
                .pluginName("系统模块")
                .pluginType(PluginType.SYSTEM)
                .runtimeMode(PluginRuntimeMode.EMBEDDED)
                .pluginVersion("1.0.0")
                .basePath("/")
                .entryJs("/plugin-static/pyin-system/assets/remoteEntry.js")
                .remoteName("pyinSystem")
                .exposedModule("./PyinSystemRemoteApp")
                .build();
    }

    @Override
    public List<PluginMenu> menus() {
        return List.of(
                route("dashboard", "控制台", "/", "LayoutDashboard", 0, "system:view"),
                route("users", "用户管理", "/users", "Users", 10, "user:view"),
                route("roles", "角色管理", "/roles", "ShieldCheck", 20, "role:view"),
                route("permissions", "权限管理", "/permissions", "KeyRound", 30, "system:view"),
                route("plugins", "插件管理", "/plugins", "Blocks", 40, "system:view"),
                route("credentials", "接入凭证", "/credentials", "BadgeCheck", 50, "system:view"),
                route("settings", "系统设置", "/settings", "Settings2", 60, "system:view"),
                new PluginMenu(
                        null,
                        "系统设置",
                        PluginMenuType.DIRECTORY,
                        null,
                        null,
                        null,
                        70,
                        null,
                        null,
                        null,
                        List.of(
                                new PluginMenu(
                                        "system",
                                        "日报",
                                        PluginMenuType.LINK,
                                        null,
                                        "http://192.168.0.166:82/workreport/pm/workHourStat",
                                        "LayoutGrid",
                                        70,
                                        "system:view",
                                        null,
                                        null,
                                        List.of()
                                )
                        )
                )
        );
    }

    private PluginMenu route(String code, String name, String path, String icon, int sort, String permissionCode) {
        return new PluginMenu(
                code,
                name,
                PluginMenuType.ROUTE,
                path,
                null,
                icon,
                sort,
                permissionCode,
                null,
                null,
                List.of()
        );
    }
}
