package com.pyin.plugin.system.permission.service;


import com.pyin.plugin.system.permission.model.PermissionSummary;
import java.util.List;
import java.util.Set;

public interface PermissionService {

    List<PermissionSummary> findAll();

    Set<String> findPermissionCodesByUserId(Long userId);
}
