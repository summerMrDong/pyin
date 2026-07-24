package com.pyin.center.auth.admin.service;

import com.pyin.plugin.system.api.model.SystemUserAuthInfo;
import com.pyin.plugin.system.api.service.SystemUserPublicService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * 后台用户登录认证服务。
 *
 * <p>该服务只负责读取 system 提供的用户认证事实并校验密码，不负责创建、销毁或读取
 * Sa-Token 会话。</p>
 */
@Service
public class AdminAuthenticationService {

    private final SystemUserPublicService systemUserPublicService;
    private final PasswordEncoder passwordEncoder;

    public AdminAuthenticationService(
            SystemUserPublicService systemUserPublicService,
            PasswordEncoder passwordEncoder
    ) {
        this.systemUserPublicService = systemUserPublicService;
        this.passwordEncoder = passwordEncoder;
    }

    public SystemUserAuthInfo authenticate(String username, String password) {
        SystemUserAuthInfo user = systemUserPublicService.findAuthInfoByUsername(username);
        if (user == null || !user.enabled() || !passwordEncoder.matches(password, user.passwordHash())) {
            return null;
        }
        return user;
    }
}
