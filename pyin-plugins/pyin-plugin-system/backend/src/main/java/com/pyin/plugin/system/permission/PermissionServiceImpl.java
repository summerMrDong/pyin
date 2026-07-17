package com.pyin.plugin.system.permission;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pyin.plugin.system.plugin.PluginEntity;
import com.pyin.plugin.system.plugin.PluginRepository;
import com.pyin.plugin.system.role.RolePermissionEntity;
import com.pyin.plugin.system.role.RolePermissionRepository;
import com.pyin.plugin.system.user.UserRoleEntity;
import com.pyin.plugin.system.user.UserRoleRepository;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class PermissionServiceImpl implements PermissionService {

    private final PermissionRepository permissionRepository;
    private final PluginPermissionRepository pluginPermissionRepository;
    private final PluginRepository pluginRepository;
    private final UserRoleRepository userRoleRepository;
    private final RolePermissionRepository rolePermissionRepository;

    public PermissionServiceImpl(
            PermissionRepository permissionRepository,
            PluginPermissionRepository pluginPermissionRepository,
            PluginRepository pluginRepository,
            UserRoleRepository userRoleRepository,
            RolePermissionRepository rolePermissionRepository
    ) {
        this.permissionRepository = permissionRepository;
        this.pluginPermissionRepository = pluginPermissionRepository;
        this.pluginRepository = pluginRepository;
        this.userRoleRepository = userRoleRepository;
        this.rolePermissionRepository = rolePermissionRepository;
    }

    @Override
    public List<PermissionSummary> findAll() {
        java.util.Map<String, String> pluginNameMap = pluginRepository.selectList(null).stream()
                .collect(java.util.stream.Collectors.toMap(
                        PluginEntity::getPluginId,
                        plugin -> plugin.getPluginName() == null || plugin.getPluginName().isBlank()
                                ? plugin.getPluginId()
                                : plugin.getPluginName().trim(),
                        (left, right) -> left,
                        java.util.LinkedHashMap::new
                ));
        List<PermissionSummary> systemPermissions = permissionRepository.selectList(null).stream()
                .map(permission -> new PermissionSummary(
                        permission.getCode(),
                        permission.getName(),
                        "SYSTEM",
                        null,
                        null,
                        "SYSTEM"
                ))
                .toList();

        List<PermissionSummary> pluginPermissions = pluginPermissionRepository.selectList(null).stream()
                .map(permission -> new PermissionSummary(
                        permission.getPermissionCode(),
                        permission.getPermissionName(),
                        "PLUGIN",
                        permission.getPluginId(),
                        pluginNameMap.getOrDefault(permission.getPluginId(), permission.getPluginId()),
                        permission.getResourceType()
                ))
                .toList();

        return java.util.stream.Stream.concat(systemPermissions.stream(), pluginPermissions.stream())
                .toList();
    }

    @Override
    public Set<String> findPermissionCodesByUserId(Long userId) {
        if (userId == null) {
            return Set.of();
        }
        List<Long> roleIds = userRoleRepository.selectList(new LambdaQueryWrapper<UserRoleEntity>()
                        .eq(UserRoleEntity::getUserId, userId))
                .stream()
                .map(UserRoleEntity::getRoleId)
                .distinct()
                .toList();
        if (roleIds.isEmpty()) {
            return Set.of();
        }
        return rolePermissionRepository.selectList(new LambdaQueryWrapper<RolePermissionEntity>()
                        .in(RolePermissionEntity::getRoleId, roleIds))
                .stream()
                .map(RolePermissionEntity::getPermissionCode)
                .filter(code -> code != null && !code.isBlank())
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }
}
