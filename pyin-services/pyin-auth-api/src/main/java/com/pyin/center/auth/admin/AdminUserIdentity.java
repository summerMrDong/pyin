package com.pyin.center.auth.admin;

public record AdminUserIdentity(
        Long id,
        String username,
        String displayName,
        String passwordHash,
        boolean enabled
) {
}
