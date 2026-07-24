package com.pyin.plugin.system.permission.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pyin.plugin.system.api.service.SystemAccessPublicService;
import com.pyin.plugin.system.api.model.SystemResourceView;
import com.pyin.plugin.system.api.model.SystemRoleView;
import com.pyin.plugin.system.permission.service.PermissionService;
import com.pyin.plugin.system.plugin.entity.PluginResourceEntity;
import com.pyin.plugin.system.plugin.repository.PluginResourceRepository;
import com.pyin.plugin.system.role.entity.RoleEntity;
import com.pyin.plugin.system.role.repository.RoleRepository;
import com.pyin.plugin.system.resource.entity.RoleResourceEntity;
import com.pyin.plugin.system.resource.repository.RoleResourceRepository;
import com.pyin.plugin.system.resource.support.SystemResourceCatalog;
import com.pyin.plugin.system.resource.support.SystemResourceDefinition;
import com.pyin.plugin.system.user.entity.UserEntity;
import com.pyin.plugin.system.user.entity.UserRoleEntity;
import com.pyin.plugin.system.user.repository.UserRoleRepository;
import com.pyin.plugin.system.user.service.UserService;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class SystemAccessPublicServiceImpl implements SystemAccessPublicService {

    private static final String SYSTEM_SCOPE = "SYSTEM";
    private static final String PLUGIN_SCOPE = "PLUGIN";

    private final PermissionService permissionService;
    private final UserService userService;
    private final UserRoleRepository userRoleRepository;
    private final RoleRepository roleRepository;
    private final RoleResourceRepository roleResourceRepository;
    private final PluginResourceRepository pluginResourceRepository;
    private final SystemResourceCatalog systemResourceCatalog;

    public SystemAccessPublicServiceImpl(
            PermissionService permissionService,
            UserService userService,
            UserRoleRepository userRoleRepository,
            RoleRepository roleRepository,
            RoleResourceRepository roleResourceRepository,
            PluginResourceRepository pluginResourceRepository,
            SystemResourceCatalog systemResourceCatalog
    ) {
        this.permissionService = permissionService;
        this.userService = userService;
        this.userRoleRepository = userRoleRepository;
        this.roleRepository = roleRepository;
        this.roleResourceRepository = roleResourceRepository;
        this.pluginResourceRepository = pluginResourceRepository;
        this.systemResourceCatalog = systemResourceCatalog;
    }

    @Override
    public Set<String> findPermissionCodesByUserId(Long userId) {
        return findEnabledUser(userId) == null ? Set.of() : permissionService.findPermissionCodesByUserId(userId);
    }

    @Override
    public List<SystemRoleView> findRolesByUserId(Long userId) {
        List<Long> roleIds = findRoleIds(userId);
        if (roleIds.isEmpty()) {
            return List.of();
        }
        return roleRepository.selectBatchIds(roleIds).stream()
                .filter(role -> StringUtils.hasText(role.getCode()))
                .map(role -> new SystemRoleView(role.getId(), role.getCode(), role.getName()))
                .sorted(Comparator.comparing(SystemRoleView::code))
                .toList();
    }

    @Override
    public Set<String> findResourceKeysByUserId(Long userId) {
        return findResourcesByUserId(userId).stream()
                .map(SystemResourceView::resourceKey)
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
    }

    @Override
    public List<SystemResourceView> findResourcesByUserId(Long userId) {
        List<Long> roleIds = findRoleIds(userId);
        if (roleIds.isEmpty()) {
            return List.of();
        }
        Set<String> grantedKeys = roleResourceRepository.selectList(new LambdaQueryWrapper<RoleResourceEntity>()
                        .in(RoleResourceEntity::getRoleId, roleIds))
                .stream()
                .map(this::toResourceKey)
                .filter(StringUtils::hasText)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        if (grantedKeys.isEmpty()) {
            return List.of();
        }
        Map<String, SystemResourceView> resources = resourceCatalog();
        return grantedKeys.stream()
                .map(resources::get)
                .filter(java.util.Objects::nonNull)
                .sorted(Comparator.comparing(SystemResourceView::resourceKey))
                .toList();
    }

    private List<Long> findRoleIds(Long userId) {
        if (findEnabledUser(userId) == null) {
            return List.of();
        }
        return userRoleRepository.selectList(new LambdaQueryWrapper<UserRoleEntity>()
                        .eq(UserRoleEntity::getUserId, userId))
                .stream()
                .map(UserRoleEntity::getRoleId)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .toList();
    }

    private UserEntity findEnabledUser(Long userId) {
        if (userId == null) {
            return null;
        }
        UserEntity user = userService.findById(userId);
        return user != null && UserService.STATUS_ENABLED.equals(user.getStatus()) ? user : null;
    }

    private Map<String, SystemResourceView> resourceCatalog() {
        Map<String, SystemResourceView> resources = new LinkedHashMap<>();
        for (SystemResourceDefinition definition : systemResourceCatalog.definitions()) {
            String resourceKey = SYSTEM_SCOPE + ":" + definition.resourceCode();
            resources.put(resourceKey, new SystemResourceView(
                    resourceKey,
                    SYSTEM_SCOPE,
                    null,
                    definition.resourceCode(),
                    definition.permissionCode()
            ));
        }
        for (PluginResourceEntity resource : pluginResourceRepository.selectList(null)) {
            if (!StringUtils.hasText(resource.getPluginId()) || !StringUtils.hasText(resource.getResourceCode())) {
                continue;
            }
            String pluginId = resource.getPluginId().trim();
            String resourceCode = resource.getResourceCode().trim();
            String resourceKey = PLUGIN_SCOPE + ":" + pluginId + "/" + resourceCode;
            resources.put(resourceKey, new SystemResourceView(
                    resourceKey,
                    PLUGIN_SCOPE,
                    pluginId,
                    resourceCode,
                    resource.getPermissionCode()
            ));
        }
        return resources;
    }

    private String toResourceKey(RoleResourceEntity resource) {
        if (!StringUtils.hasText(resource.getResourceScope()) || !StringUtils.hasText(resource.getResourceCode())) {
            return null;
        }
        return resource.getResourceScope().trim() + ":" + resource.getResourceCode().trim();
    }
}
