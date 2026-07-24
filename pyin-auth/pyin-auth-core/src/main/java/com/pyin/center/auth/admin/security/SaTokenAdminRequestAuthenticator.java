package com.pyin.center.auth.admin.security;

import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.exception.NotPermissionException;
import cn.dev33.satoken.stp.StpUtil;
import com.pyin.center.auth.authentication.AdminRequestAuthenticator;
import com.pyin.center.auth.authentication.AuthenticatedPrincipal;
import com.pyin.center.auth.authentication.AuthenticationException;
import com.pyin.center.auth.authentication.AuthorizationException;
import com.pyin.plugin.system.api.model.SystemUserView;
import com.pyin.plugin.system.api.service.SystemUserPublicService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class SaTokenAdminRequestAuthenticator implements AdminRequestAuthenticator {

    private static final String SUPER_ADMIN_ROLE_CODE = "ADMIN";

    private final SystemUserPublicService systemUserPublicService;

    public SaTokenAdminRequestAuthenticator(SystemUserPublicService systemUserPublicService) {
        this.systemUserPublicService = systemUserPublicService;
    }

    @Override
    public void checkLogin() {
        try {
            StpUtil.checkLogin();
        } catch (NotLoginException exception) {
            throw new AuthenticationException("未登录或登录已过期", exception);
        }
    }

    @Override
    public AuthenticatedPrincipal authenticate(String permissionCode) {
        checkLogin();
        SystemUserView user = systemUserPublicService.findUserById(StpUtil.getLoginIdAsLong());
        if (user == null || !user.enabled()) {
            throw new AuthenticationException("当前后台用户不存在或已被禁用");
        }
        if (StringUtils.hasText(permissionCode) && !StpUtil.hasRole(SUPER_ADMIN_ROLE_CODE)) {
            try {
                StpUtil.checkPermission(permissionCode);
            } catch (NotPermissionException exception) {
                throw new AuthorizationException("缺少插件接口权限：" + permissionCode, exception);
            }
        }
        String displayName = StringUtils.hasText(user.displayName()) ? user.displayName() : user.username();
        return AuthenticatedPrincipal.adminUser(user.id(), displayName);
    }
}
