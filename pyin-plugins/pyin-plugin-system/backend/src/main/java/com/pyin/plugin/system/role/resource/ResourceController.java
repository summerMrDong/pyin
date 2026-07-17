package com.pyin.plugin.system.role.resource;

import com.pyin.plugin.common.api.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/resources")
public class ResourceController {

    private final RoleResourceService roleResourceService;

    public ResourceController(RoleResourceService roleResourceService) {
        this.roleResourceService = roleResourceService;
    }

    @GetMapping("/tree")
    public Result<ResourceTreeResponse> tree() {
        return Result.ok(roleResourceService.findResourceTree());
    }
}
