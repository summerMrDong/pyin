package com.pyin.center.auth.admin;

public interface AdminUserAuthenticationProvider {

    AdminUserIdentity findByUsername(String username);

    AdminUserIdentity findById(Long userId);
}
