package com.pyin.plugin.common.security;

public interface PluginAdminAccessService {

    PluginAdminAccessDecision checkAccess(String pluginId, String method, String path);
}
