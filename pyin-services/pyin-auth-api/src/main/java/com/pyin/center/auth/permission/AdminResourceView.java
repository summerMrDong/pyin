package com.pyin.center.auth.permission;

public record AdminResourceView(
        String resourceKey,
        String scope,
        String pluginId,
        String resourceCode,
        String permissionCode
) {
}
