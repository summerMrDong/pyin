package com.pyin.plugin.system.role.support;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pyin.plugin.system.role.entity.RolePermissionEntity;
import com.pyin.plugin.system.role.repository.RolePermissionRepository;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class RolePermissionBindingSupport {

    private final RolePermissionRepository rolePermissionRepository;

    public RolePermissionBindingSupport(RolePermissionRepository rolePermissionRepository) {
        this.rolePermissionRepository = rolePermissionRepository;
    }

    public List<String> findPermissionCodes(Long roleId) {
        return rolePermissionRepository.selectList(new LambdaQueryWrapper<RolePermissionEntity>()
                        .eq(RolePermissionEntity::getRoleId, roleId))
                .stream()
                .map(RolePermissionEntity::getPermissionCode)
                .filter(code -> code != null && !code.isBlank())
                .distinct()
                .toList();
    }

    public Map<Long, List<String>> buildPermissionMap(List<Long> roleIds) {
        if (roleIds == null || roleIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, List<String>> result = new LinkedHashMap<>();
        for (RolePermissionEntity relation : rolePermissionRepository.selectList(new LambdaQueryWrapper<RolePermissionEntity>()
                .in(RolePermissionEntity::getRoleId, roleIds))) {
            if (!StringUtils.hasText(relation.getPermissionCode())) {
                continue;
            }
            result.computeIfAbsent(relation.getRoleId(), key -> new ArrayList<>()).add(relation.getPermissionCode());
        }
        result.values().forEach(list -> list.sort(String::compareTo));
        return result;
    }

    public void replacePermissions(Long roleId, List<String> permissionCodes) {
        rolePermissionRepository.delete(new LambdaQueryWrapper<RolePermissionEntity>()
                .eq(RolePermissionEntity::getRoleId, roleId));
        for (String permissionCode : sanitizePermissionCodes(permissionCodes)) {
            RolePermissionEntity entity = new RolePermissionEntity();
            entity.setRoleId(roleId);
            entity.setPermissionCode(permissionCode);
            rolePermissionRepository.insert(entity);
        }
    }

    public void deletePermissions(Long roleId) {
        rolePermissionRepository.delete(new LambdaQueryWrapper<RolePermissionEntity>()
                .eq(RolePermissionEntity::getRoleId, roleId));
    }

    private List<String> sanitizePermissionCodes(List<String> permissionCodes) {
        if (permissionCodes == null || permissionCodes.isEmpty()) {
            return List.of();
        }
        Set<String> codes = new LinkedHashSet<>();
        for (String permissionCode : permissionCodes) {
            if (StringUtils.hasText(permissionCode)) {
                codes.add(permissionCode.trim());
            }
        }
        return new ArrayList<>(codes);
    }
}
