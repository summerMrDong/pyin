package com.pyin.center.auth.permission;

import java.util.List;
import java.util.Set;

public interface AdminAccessProvider {

    List<AdminRoleView> findRolesByUserId(Long userId);

    Set<String> findResourceKeysByUserId(Long userId);

    List<AdminResourceView> findResourcesByUserId(Long userId);
}
