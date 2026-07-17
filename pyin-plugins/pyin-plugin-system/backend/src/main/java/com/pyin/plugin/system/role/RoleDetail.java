package com.pyin.plugin.system.role;

import java.time.LocalDateTime;
import java.util.List;

public record RoleDetail(
        Long id,
        String code,
        String name,
        String description,
        Integer sort,
        List<String> permissionCodes,
        int permissionCount,
        int userCount,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
