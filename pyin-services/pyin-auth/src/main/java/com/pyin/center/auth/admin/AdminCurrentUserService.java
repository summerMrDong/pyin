package com.pyin.center.auth.admin;

import cn.dev33.satoken.stp.StpUtil;
import com.pyin.center.auth.permission.AdminAccessProvider;
import com.pyin.center.auth.permission.AdminPermissionProvider;
import com.pyin.center.auth.permission.AdminResourceView;
import com.pyin.center.auth.permission.AdminRoleView;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class AdminCurrentUserService {

    private final AdminUserAccessProvider adminUserAccessProvider;
    private final AdminPermissionProvider adminPermissionProvider;
    private final AdminAccessProvider adminAccessProvider;

    public AdminCurrentUserService(
            AdminUserAccessProvider adminUserAccessProvider,
            AdminPermissionProvider adminPermissionProvider,
            AdminAccessProvider adminAccessProvider
    ) {
        this.adminUserAccessProvider = adminUserAccessProvider;
        this.adminPermissionProvider = adminPermissionProvider;
        this.adminAccessProvider = adminAccessProvider;
    }

    public Optional<AdminUserView> currentUser() {
        Long userId = currentUserId();
        return userId == null ? Optional.empty() : Optional.ofNullable(adminUserAccessProvider.findById(userId));
    }

    public Set<String> currentPermissionCodes() {
        Long userId = currentUserId();
        return userId == null ? Set.of() : adminPermissionProvider.findPermissionCodesByUserId(userId);
    }

    public boolean hasPermission(String permissionCode) {
        return permissionCode != null && currentPermissionCodes().contains(permissionCode);
    }

    public List<AdminRoleView> currentRoles() {
        Long userId = currentUserId();
        return userId == null ? List.of() : adminAccessProvider.findRolesByUserId(userId);
    }

    public List<AdminResourceView> currentResources() {
        Long userId = currentUserId();
        return userId == null ? List.of() : adminAccessProvider.findResourcesByUserId(userId);
    }

    public Set<String> currentResourceKeys() {
        Long userId = currentUserId();
        return userId == null ? Set.of() : adminAccessProvider.findResourceKeysByUserId(userId);
    }

    private Long currentUserId() {
        return StpUtil.isLogin() ? StpUtil.getLoginIdAsLong() : null;
    }
}
