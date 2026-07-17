package com.pyin.plugin.system.user;

import java.time.LocalDateTime;
import java.util.List;

public record UserDetail(
        Long id,
        String username,
        String displayName,
        String status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        List<Long> roleIds,
        List<UserRoleView> roles
) {
}
