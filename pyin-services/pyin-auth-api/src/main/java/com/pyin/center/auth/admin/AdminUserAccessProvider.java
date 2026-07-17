package com.pyin.center.auth.admin;

public interface AdminUserAccessProvider {

    AdminUserView findById(Long userId);
}
