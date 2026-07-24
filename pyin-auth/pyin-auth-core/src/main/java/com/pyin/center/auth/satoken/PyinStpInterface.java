package com.pyin.center.auth.satoken;

import cn.dev33.satoken.stp.StpInterface;
import com.pyin.plugin.system.api.service.SystemAccessPublicService;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class PyinStpInterface implements StpInterface {

    private final SystemAccessPublicService systemAccessPublicService;

    public PyinStpInterface(SystemAccessPublicService systemAccessPublicService) {
        this.systemAccessPublicService = systemAccessPublicService;
    }

    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        Long userId = parseUserId(loginId);
        if (userId == null) {
            return List.of();
        }
        Set<String> permissionCodes = systemAccessPublicService.findPermissionCodesByUserId(userId);
        return permissionCodes.stream().toList();
    }

    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        Long userId = parseUserId(loginId);
        if (userId == null) {
            return List.of();
        }
        return systemAccessPublicService.findRolesByUserId(userId).stream()
                .map(role -> role.code())
                .toList();
    }

    private Long parseUserId(Object loginId) {
        if (loginId instanceof Long value) {
            return value;
        }
        if (loginId instanceof Number value) {
            return value.longValue();
        }
        if (loginId instanceof String value && value.matches("\\d+")) {
            return Long.parseLong(value);
        }
        return null;
    }
}
