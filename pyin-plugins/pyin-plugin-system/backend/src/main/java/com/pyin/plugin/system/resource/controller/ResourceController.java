package com.pyin.plugin.system.resource.controller;


import com.pyin.plugin.system.resource.model.ResourceTreeResponse;
import com.pyin.plugin.system.resource.service.RoleResourceService;
import com.pyin.plugin.common.api.Result;
import com.pyin.plugin.sdk.annotation.AdminMapping;
import com.pyin.plugin.sdk.annotation.Permission;
import org.springframework.web.bind.annotation.GetMapping;

@AdminMapping("/resources")
public class ResourceController {

    private final RoleResourceService roleResourceService;

    public ResourceController(RoleResourceService roleResourceService) {
        this.roleResourceService = roleResourceService;
    }

    @Permission(code = "system:view", name = "系统查看")
    @GetMapping("/tree")
    public Result<ResourceTreeResponse> tree() {
        return Result.ok(roleResourceService.findResourceTree());
    }
}
