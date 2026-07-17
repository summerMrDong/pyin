package com.pyin.plugin.system.role.resource;

import java.util.List;

public interface RoleResourceService {

    ResourceTreeResponse findResourceTree();

    List<String> findRoleResourceKeys(Long roleId);

    void replaceRoleResources(Long roleId, List<String> resourceKeys);
}
