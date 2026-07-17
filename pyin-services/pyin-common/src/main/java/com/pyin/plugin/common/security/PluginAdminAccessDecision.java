package com.pyin.plugin.common.security;

public record PluginAdminAccessDecision(
        boolean allowed,
        String permissionCode,
        String code,
        String message
) {

    public static PluginAdminAccessDecision allow(String permissionCode) {
        return new PluginAdminAccessDecision(true, permissionCode, "OK", "success");
    }

    public static PluginAdminAccessDecision deny(String code, String message) {
        return new PluginAdminAccessDecision(false, null, code, message);
    }
}
