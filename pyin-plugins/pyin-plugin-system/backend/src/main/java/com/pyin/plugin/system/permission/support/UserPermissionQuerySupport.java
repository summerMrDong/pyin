package com.pyin.plugin.system.permission.support;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pyin.plugin.system.role.entity.RolePermissionEntity;
import com.pyin.plugin.system.role.repository.RolePermissionRepository;
import com.pyin.plugin.system.user.entity.UserRoleEntity;
import com.pyin.plugin.system.user.repository.UserRoleRepository;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class UserPermissionQuerySupport {

    private final UserRoleRepository userRoleRepository;
    private final RolePermissionRepository rolePermissionRepository;

    public UserPermissionQuerySupport(
            UserRoleRepository userRoleRepository,
            RolePermissionRepository rolePermissionRepository
    ) {
        this.userRoleRepository = userRoleRepository;
        this.rolePermissionRepository = rolePermissionRepository;
    }

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
