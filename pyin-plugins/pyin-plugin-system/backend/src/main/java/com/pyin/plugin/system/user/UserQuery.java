package com.pyin.plugin.system.user;

public record UserQuery(
        String username,
        String displayName,
        String status
) {
}
