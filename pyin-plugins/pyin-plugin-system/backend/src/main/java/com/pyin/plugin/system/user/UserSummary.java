package com.pyin.plugin.system.user;

import java.time.LocalDateTime;
import java.util.List;

public record UserSummary(
        Long id,
        String username,
        String displayName,
        String status,
        LocalDateTime createdAt,
        List<UserRoleView> roles
) {
}
