package com.pyin.plugin.system.user.model;

public record UserQuery(
        String username,
        String displayName,
        String status
) {
}
