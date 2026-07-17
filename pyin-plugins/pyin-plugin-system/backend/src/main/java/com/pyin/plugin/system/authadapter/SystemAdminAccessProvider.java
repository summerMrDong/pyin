package com.pyin.plugin.system.authadapter;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pyin.center.auth.admin.AdminUserAccessProvider;
import com.pyin.center.auth.admin.AdminUserView;
import com.pyin.center.auth.permission.AdminAccessProvider;
import com.pyin.center.auth.permission.AdminResourceView;
import com.pyin.center.auth.permission.AdminRoleView;
import com.pyin.plugin.system.plugin.PluginResourceEntity;
import com.pyin.plugin.system.plugin.PluginResourceRepository;
import com.pyin.plugin.system.role.RoleEntity;
import com.pyin.plugin.system.role.RoleRepository;
import com.pyin.plugin.system.role.resource.RoleResourceEntity;
import com.pyin.plugin.system.role.resource.RoleResourceRepository;
import com.pyin.plugin.system.role.resource.SystemResourceCatalog;
import com.pyin.plugin.system.role.resource.SystemResourceDefinition;
import com.pyin.plugin.system.user.UserEntity;
import com.pyin.plugin.system.user.UserRoleEntity;
import com.pyin.plugin.system.user.UserRoleRepository;
import com.pyin.plugin.system.user.UserService;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class SystemAdminAccessProvider implements AdminUserAccessProvider, AdminAccessProvider {

    private static final String SYSTEM_SCOPE = "SYSTEM";
    private static final String PLUGIN_SCOPE = "PLUGIN";

    private final UserService userService;
    private final UserRoleRepository userRoleRepository;
    private final RoleRepository roleRepository;
    private final RoleResourceRepository roleResourceRepository;
    private final PluginResourceRepository pluginResourceRepository;
    private final SystemResourceCatalog systemResourceCatalog;

    public SystemAdminAccessProvider(
            UserService userService,
            UserRoleRepository userRoleRepository,
            RoleRepository roleRepository,
            RoleResourceRepository roleResourceRepository,
            PluginResourceRepository pluginResourceRepository,
            SystemResourceCatalog systemResourceCatalog
    ) {
        this.userService = userService;
        this.userRoleRepository = userRoleRepository;
        this.roleRepository = roleRepository;
        this.roleResourceRepository = roleResourceRepository;
        this.pluginResourceRepository = pluginResourceRepository;
        this.systemResourceCatalog = systemResourceCatalog;
    }

    @Override
    public AdminUserView findById(Long userId) {
        UserEntity user = findEnabledUser(userId);
        if (user == null) {
            return null;
        }
        return new AdminUserView(user.getId(), user.getUsername(), user.getDisplayName(), true);
    }

    @Override
    public List<AdminRoleView> findRolesByUserId(Long userId) {
        List<Long> roleIds = findRoleIds(userId);
        if (roleIds.isEmpty()) {
            return List.of();
        }
        return roleRepository.selectBatchIds(roleIds).stream()
                .filter(role -> StringUtils.hasText(role.getCode()))
                .map(role -> new AdminRoleView(role.getId(), role.getCode(), role.getName()))
                .sorted(Comparator.comparing(AdminRoleView::code))
                .toList();
    }

    @Override
    public Set<String> findResourceKeysByUserId(Long userId) {
        return findResourcesByUserId(userId).stream()
                .map(AdminResourceView::resourceKey)
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
    }

    @Override
    public List<AdminResourceView> findResourcesByUserId(Long userId) {
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
        Map<String, AdminResourceView> resources = resourceCatalog();
        return grantedKeys.stream()
                .map(resources::get)
                .filter(java.util.Objects::nonNull)
                .sorted(Comparator.comparing(AdminResourceView::resourceKey))
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

    private Map<String, AdminResourceView> resourceCatalog() {
        Map<String, AdminResourceView> resources = new LinkedHashMap<>();
        for (SystemResourceDefinition definition : systemResourceCatalog.definitions()) {
            String resourceKey = SYSTEM_SCOPE + ":" + definition.resourceCode();
            resources.put(resourceKey, new AdminResourceView(
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
            String resourceKey = PLUGIN_SCOPE + ":" + resource.getPluginId().trim() + "/" + resource.getResourceCode().trim();
            resources.put(resourceKey, new AdminResourceView(
                    resourceKey,
                    PLUGIN_SCOPE,
                    resource.getPluginId().trim(),
                    resource.getResourceCode().trim(),
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
