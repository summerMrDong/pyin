package com.pyin.plugin.system.resource.service;


import com.pyin.plugin.system.resource.model.ResourceTreeResponse;
import java.util.List;

public interface RoleResourceService {

    ResourceTreeResponse findResourceTree();

    List<String> findRoleResourceKeys(Long roleId);

    void replaceRoleResources(Long roleId, List<String> resourceKeys);
}
