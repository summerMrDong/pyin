package com.pyin.plugin.system.resource.service.impl;

import com.pyin.plugin.system.resource.model.ResourceTreeResponse;
import com.pyin.plugin.system.resource.service.RoleResourceService;
import com.pyin.plugin.system.resource.support.ResourceTreeBuilder;
import com.pyin.plugin.system.resource.support.RoleResourceBindingSupport;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RoleResourceServiceImpl implements RoleResourceService {

    private final ResourceTreeBuilder resourceTreeBuilder;
    private final RoleResourceBindingSupport roleResourceBindingSupport;

    public RoleResourceServiceImpl(
            ResourceTreeBuilder resourceTreeBuilder,
            RoleResourceBindingSupport roleResourceBindingSupport
    ) {
        this.resourceTreeBuilder = resourceTreeBuilder;
        this.roleResourceBindingSupport = roleResourceBindingSupport;
    }

    @Override
    public ResourceTreeResponse findResourceTree() {
        return resourceTreeBuilder.buildResourceTree();
    }

    @Override
    public List<String> findRoleResourceKeys(Long roleId) {
        return roleResourceBindingSupport.findRoleResourceKeys(roleId);
    }

    @Override
    @Transactional
    public void replaceRoleResources(Long roleId, List<String> resourceKeys) {
        roleResourceBindingSupport.replaceRoleResources(roleId, resourceKeys);
    }
}
