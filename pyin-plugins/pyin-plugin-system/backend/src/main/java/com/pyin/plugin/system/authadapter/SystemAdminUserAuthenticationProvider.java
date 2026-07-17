package com.pyin.plugin.system.authadapter;

import com.pyin.center.auth.admin.AdminUserAuthenticationProvider;
import com.pyin.center.auth.admin.AdminUserIdentity;
import com.pyin.plugin.system.user.UserEntity;
import com.pyin.plugin.system.user.UserService;
import org.springframework.stereotype.Component;

@Component
public class SystemAdminUserAuthenticationProvider implements AdminUserAuthenticationProvider {

    private final UserService userService;

    public SystemAdminUserAuthenticationProvider(UserService userService) {
        this.userService = userService;
    }

    @Override
    public AdminUserIdentity findByUsername(String username) {
        return toIdentity(userService.findByUsername(username));
    }

    @Override
    public AdminUserIdentity findById(Long userId) {
        return toIdentity(userService.findById(userId));
    }

    private AdminUserIdentity toIdentity(UserEntity user) {
        if (user == null) {
            return null;
        }
        return new AdminUserIdentity(
                user.getId(),
                user.getUsername(),
                user.getDisplayName(),
                user.getPasswordHash(),
                UserService.STATUS_ENABLED.equals(user.getStatus())
        );
    }
}
