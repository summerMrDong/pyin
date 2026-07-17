package com.pyin.center.auth.admin;

import com.pyin.center.auth.crypto.PasswordHasher;
import org.springframework.stereotype.Service;

@Service
public class AdminAuthService {

    private final AdminUserAuthenticationProvider adminUserAuthenticationProvider;
    private final PasswordHasher passwordHasher;

    public AdminAuthService(
            AdminUserAuthenticationProvider adminUserAuthenticationProvider,
            PasswordHasher passwordHasher
    ) {
        this.adminUserAuthenticationProvider = adminUserAuthenticationProvider;
        this.passwordHasher = passwordHasher;
    }

    public AdminUserIdentity authenticate(String username, String password) {
        AdminUserIdentity user = adminUserAuthenticationProvider.findByUsername(username);
        if (user == null || !user.enabled() || !passwordHasher.matches(password, user.passwordHash())) {
            return null;
        }
        return user;
    }

    public AdminUserIdentity currentUser(Long loginId) {
        return adminUserAuthenticationProvider.findById(loginId);
    }
}
