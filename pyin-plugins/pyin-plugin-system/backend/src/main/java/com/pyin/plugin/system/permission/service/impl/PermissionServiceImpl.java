package com.pyin.plugin.system.permission.service.impl;

import com.pyin.plugin.system.permission.model.PermissionSummary;
import com.pyin.plugin.system.permission.service.PermissionService;
import com.pyin.plugin.system.permission.support.PermissionCatalogSupport;
import com.pyin.plugin.system.permission.support.UserPermissionQuerySupport;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class PermissionServiceImpl implements PermissionService {

    private final PermissionCatalogSupport permissionCatalogSupport;
    private final UserPermissionQuerySupport userPermissionQuerySupport;

    public PermissionServiceImpl(
            PermissionCatalogSupport permissionCatalogSupport,
            UserPermissionQuerySupport userPermissionQuerySupport
    ) {
        this.permissionCatalogSupport = permissionCatalogSupport;
        this.userPermissionQuerySupport = userPermissionQuerySupport;
    }

    @Override
    public List<PermissionSummary> findAll() {
        return permissionCatalogSupport.findAll();
    }

    @Override
    public Set<String> findPermissionCodesByUserId(Long userId) {
        return userPermissionQuerySupport.findPermissionCodesByUserId(userId);
    }
}
