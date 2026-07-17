package com.pyin.plugin.system.permission;

import java.util.List;
import java.util.Set;

public interface PermissionService {

    List<PermissionSummary> findAll();

    Set<String> findPermissionCodesByUserId(Long userId);
}
