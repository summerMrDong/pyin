package com.pyin.plugin.system.permission.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
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
import com.pyin.plugin.system.user.entity.UserEntity;
import com.pyin.plugin.system.user.entity.UserRoleEntity;
import com.pyin.plugin.system.user.repository.UserRoleRepository;
import com.pyin.plugin.system.user.service.UserService;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class SystemAccessPublicServiceImplTest {

    @Mock
    private PermissionService permissionService;
    @Mock
    private UserService userService;
    @Mock
    private UserRoleRepository userRoleRepository;
    @Mock
    private RoleRepository roleRepository;
    @Mock
    private RoleResourceRepository roleResourceRepository;
    @Mock
    private PluginResourceRepository pluginResourceRepository;

    private SystemAccessPublicServiceImpl publicService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        publicService = new SystemAccessPublicServiceImpl(
                permissionService,
                userService,
                userRoleRepository,
                roleRepository,
                roleResourceRepository,
                pluginResourceRepository,
                new SystemResourceCatalog()
        );
    }

    @Test
    void shouldExposePermissionsForEnabledUser() {
        when(userService.findById(1L)).thenReturn(user(1L, UserService.STATUS_ENABLED));
        when(permissionService.findPermissionCodesByUserId(1L)).thenReturn(Set.of("user:view", "role:view"));

        assertThat(publicService.findPermissionCodesByUserId(1L)).containsExactlyInAnyOrder("user:view", "role:view");
    }

    @Test
    void shouldHidePermissionsForDisabledUser() {
        when(userService.findById(2L)).thenReturn(user(2L, UserService.STATUS_DISABLED));

        assertThat(publicService.findPermissionCodesByUserId(2L)).isEmpty();
    }

    @Test
    void shouldExposeRolesForEnabledUser() {
        when(userService.findById(1L)).thenReturn(user(1L, UserService.STATUS_ENABLED));
        when(userRoleRepository.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(userRole(1L, 20L), userRole(1L, 10L)));
        when(roleRepository.selectBatchIds(List.of(20L, 10L))).thenReturn(List.of(role(20L, "operator", "操作员"), role(10L, "admin", "管理员")));

        List<SystemRoleView> roles = publicService.findRolesByUserId(1L);

        assertThat(roles).extracting(SystemRoleView::code).containsExactly("admin", "operator");
    }

    @Test
    void shouldExposeSystemAndPluginResourcesForEnabledUser() {
        when(userService.findById(1L)).thenReturn(user(1L, UserService.STATUS_ENABLED));
        when(userRoleRepository.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(userRole(1L, 10L)));
        when(roleResourceRepository.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(
                roleResource(10L, "SYSTEM", "users"),
                roleResource(10L, "PLUGIN", "config/items")
        ));
        when(pluginResourceRepository.selectList(null)).thenReturn(List.of(pluginResource("config", "items", "config:view")));

        List<SystemResourceView> resources = publicService.findResourcesByUserId(1L);

        assertThat(resources).extracting(SystemResourceView::resourceKey)
                .containsExactly("PLUGIN:config/items", "SYSTEM:users");
        assertThat(publicService.findResourceKeysByUserId(1L))
                .containsExactlyInAnyOrder("PLUGIN:config/items", "SYSTEM:users");
    }

    private UserEntity user(Long id, String status) {
        UserEntity user = new UserEntity();
        user.setId(id);
        user.setStatus(status);
        return user;
    }

    private UserRoleEntity userRole(Long userId, Long roleId) {
        UserRoleEntity userRole = new UserRoleEntity();
        userRole.setUserId(userId);
        userRole.setRoleId(roleId);
        return userRole;
    }

    private RoleEntity role(Long id, String code, String name) {
        RoleEntity role = new RoleEntity();
        role.setId(id);
        role.setCode(code);
        role.setName(name);
        return role;
    }

    private RoleResourceEntity roleResource(Long roleId, String scope, String code) {
        RoleResourceEntity roleResource = new RoleResourceEntity();
        roleResource.setRoleId(roleId);
        roleResource.setResourceScope(scope);
        roleResource.setResourceCode(code);
        return roleResource;
    }

    private PluginResourceEntity pluginResource(String pluginId, String resourceCode, String permissionCode) {
        PluginResourceEntity resource = new PluginResourceEntity();
        resource.setPluginId(pluginId);
        resource.setResourceCode(resourceCode);
        resource.setPermissionCode(permissionCode);
        return resource;
    }
}
