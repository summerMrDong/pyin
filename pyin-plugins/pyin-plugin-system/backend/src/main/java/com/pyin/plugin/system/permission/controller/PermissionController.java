package com.pyin.plugin.system.permission.controller;


import com.pyin.plugin.system.permission.model.PermissionSummary;
import com.pyin.plugin.system.permission.service.PermissionService;
import com.pyin.plugin.common.api.Result;
import com.pyin.plugin.sdk.annotation.AdminMapping;
import com.pyin.plugin.sdk.annotation.Permission;
import com.pyin.plugin.system.role.service.RoleService;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@AdminMapping
public class PermissionController {

    private final PermissionService permissionService;
    private final RoleService roleService;

    public PermissionController(PermissionService permissionService, RoleService roleService) {
        this.permissionService = permissionService;
        this.roleService = roleService;
    }

    @Permission(code = "system:view", name = "系统查看")
    @GetMapping("/permissions")
    public Result<List<Map<String, Object>>> list() {
        return Result.ok(permissionService.findAll().stream()
                .map(permission -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("code", permission.code());
                    item.put("name", permission.name());
                    item.put("source", permission.source());
                    item.put("pluginId", permission.pluginId());
                    item.put("pluginName", permission.pluginName());
                    item.put("resourceType", permission.resourceType());
                    return item;
                })
                .toList());
    }

    @Permission(code = "role:update", name = "角色更新")
    @PostMapping("/roles/{id}/permissions")
    public Result<Void> bindRolePermissions(@PathVariable Long id, @RequestBody(required = false) List<String> permissionCodes) {
        if (!roleService.exists(id)) {
            return Result.fail("PYIN-ROLE-404", "Role not found: " + id);
        }
        List<String> requestedPermissions = permissionCodes == null ? List.of() : permissionCodes;
        List<String> knownPermissions = permissionService.findAll().stream()
                .map(PermissionSummary::code)
                .toList();
        List<String> unknownPermissions = requestedPermissions.stream()
                .filter(code -> !knownPermissions.contains(code))
                .distinct()
                .toList();
        if (!unknownPermissions.isEmpty()) {
            return Result.fail("PYIN-PERMISSION-400", "Unknown permission codes: " + String.join(",", unknownPermissions));
        }
        roleService.replacePermissions(id, requestedPermissions);
        return Result.ok();
    }
}
