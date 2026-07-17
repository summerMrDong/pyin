package com.pyin.plugin.system.role;

import com.pyin.plugin.common.api.Result;
import com.pyin.plugin.system.permission.PermissionService;
import com.pyin.plugin.system.role.resource.RoleResourceService;
import java.util.List;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/roles")
public class RoleController {

    private final RoleService roleService;
    private final PermissionService permissionService;
    private final RoleResourceService roleResourceService;

    public RoleController(
            RoleService roleService,
            PermissionService permissionService,
            RoleResourceService roleResourceService
    ) {
        this.roleService = roleService;
        this.permissionService = permissionService;
        this.roleResourceService = roleResourceService;
    }

    @GetMapping
    public Result<?> list(
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String name
    ) {
        return Result.ok(roleService.findAll(new RoleQuery(code, name)));
    }

    @GetMapping("/options")
    public Result<?> options() {
        return Result.ok(roleService.findOptions());
    }

    @GetMapping("/{id}")
    public Result<?> detail(@PathVariable Long id) {
        RoleDetail detail = roleService.findDetail(id);
        if (detail == null) {
            return Result.fail("PYIN-ROLE-404", "角色不存在: " + id);
        }
        return Result.ok(detail);
    }

    @PostMapping
    public Result<?> create(@RequestBody(required = false) CreateRoleRequest request) {
        List<String> validationErrors = validatePermissions(request == null ? null : request.getPermissionCodes());
        if (!validationErrors.isEmpty()) {
            return Result.fail("PYIN-ROLE-400", "未知权限编码: " + String.join(",", validationErrors));
        }
        try {
            return Result.ok(roleService.create(request));
        } catch (IllegalArgumentException exception) {
            return Result.fail("PYIN-ROLE-400", exception.getMessage());
        }
    }

    @PutMapping("/{id}")
    public Result<?> update(@PathVariable Long id, @RequestBody(required = false) UpdateRoleRequest request) {
        List<String> validationErrors = validatePermissions(request == null ? null : request.getPermissionCodes());
        if (!validationErrors.isEmpty()) {
            return Result.fail("PYIN-ROLE-400", "未知权限编码: " + String.join(",", validationErrors));
        }
        try {
            return Result.ok(roleService.update(id, request));
        } catch (IllegalArgumentException exception) {
            return Result.fail("PYIN-ROLE-400", exception.getMessage());
        }
    }

    @GetMapping("/{id}/permissions")
    public Result<?> permissions(@PathVariable Long id) {
        if (!roleService.exists(id)) {
            return Result.fail("PYIN-ROLE-404", "角色不存在: " + id);
        }
        return Result.ok(roleService.findPermissionCodes(id));
    }

    @PutMapping("/{id}/permissions")
    public Result<?> replacePermissions(@PathVariable Long id, @RequestBody(required = false) List<String> permissionCodes) {
        if (!roleService.exists(id)) {
            return Result.fail("PYIN-ROLE-404", "角色不存在: " + id);
        }
        List<String> validationErrors = validatePermissions(permissionCodes);
        if (!validationErrors.isEmpty()) {
            return Result.fail("PYIN-ROLE-400", "未知权限编码: " + String.join(",", validationErrors));
        }
        roleService.replacePermissions(id, permissionCodes == null ? List.of() : permissionCodes);
        return Result.ok();
    }

    @GetMapping("/{id}/resources")
    public Result<?> resources(@PathVariable Long id) {
        if (!roleService.exists(id)) {
            return Result.fail("PYIN-ROLE-404", "角色不存在: " + id);
        }
        return Result.ok(roleResourceService.findRoleResourceKeys(id));
    }

    @PutMapping("/{id}/resources")
    public Result<?> replaceResources(@PathVariable Long id, @RequestBody(required = false) List<String> resourceKeys) {
        if (!roleService.exists(id)) {
            return Result.fail("PYIN-ROLE-404", "角色不存在: " + id);
        }
        try {
            roleResourceService.replaceRoleResources(id, resourceKeys);
            return Result.ok();
        } catch (IllegalArgumentException exception) {
            return Result.fail("PYIN-RESOURCE-400", exception.getMessage());
        }
    }

    @GetMapping("/{id}/users")
    public Result<?> users(@PathVariable Long id) {
        if (!roleService.exists(id)) {
            return Result.fail("PYIN-ROLE-404", "角色不存在: " + id);
        }
        return Result.ok(roleService.findAssignedUsers(id));
    }

    @PutMapping("/{id}/users")
    public Result<?> replaceUsers(@PathVariable Long id, @RequestBody(required = false) List<Long> userIds) {
        if (!roleService.exists(id)) {
            return Result.fail("PYIN-ROLE-404", "角色不存在: " + id);
        }
        try {
            roleService.replaceUsers(id, userIds);
            return Result.ok();
        } catch (IllegalArgumentException exception) {
            return Result.fail("PYIN-ROLE-400", exception.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable Long id) {
        try {
            roleService.delete(id);
            return Result.ok();
        } catch (IllegalArgumentException exception) {
            return Result.fail("PYIN-ROLE-404", exception.getMessage());
        } catch (IllegalStateException exception) {
            return Result.fail("PYIN-ROLE-409", exception.getMessage());
        }
    }

    private List<String> validatePermissions(List<String> permissionCodes) {
        List<String> requestedPermissions = permissionCodes == null ? List.of() : permissionCodes;
        List<String> knownPermissions = permissionService.findAll().stream()
                .map(permission -> permission.code())
                .toList();
        return requestedPermissions.stream()
                .filter(code -> code != null && !code.isBlank())
                .map(String::trim)
                .filter(code -> !knownPermissions.contains(code))
                .distinct()
                .toList();
    }
}
