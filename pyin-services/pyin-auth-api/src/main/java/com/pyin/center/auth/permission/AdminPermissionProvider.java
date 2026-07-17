package com.pyin.center.auth.permission;

import java.util.Set;

public interface AdminPermissionProvider {

    Set<String> findPermissionCodesByUserId(Long userId);
}
