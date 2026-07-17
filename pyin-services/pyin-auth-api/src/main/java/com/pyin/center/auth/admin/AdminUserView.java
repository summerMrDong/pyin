package com.pyin.center.auth.admin;

public record AdminUserView(
        Long id,
        String username,
        String displayName,
        boolean enabled
) {
}
