package com.pyin.plugin.system.authadapter;

import com.pyin.center.auth.permission.AdminPermissionProvider;
import com.pyin.plugin.system.permission.PermissionService;
import com.pyin.plugin.system.user.UserEntity;
import com.pyin.plugin.system.user.UserService;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class SystemAdminPermissionProvider implements AdminPermissionProvider {

    private final PermissionService permissionService;
    private final UserService userService;

    public SystemAdminPermissionProvider(PermissionService permissionService, UserService userService) {
        this.permissionService = permissionService;
        this.userService = userService;
    }

    @Override
    public Set<String> findPermissionCodesByUserId(Long userId) {
        UserEntity user = userId == null ? null : userService.findById(userId);
        return user != null && UserService.STATUS_ENABLED.equals(user.getStatus())
                ? permissionService.findPermissionCodesByUserId(userId)
                : Set.of();
    }
}
