package com.pyin.plugin.system.user.support;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pyin.plugin.system.role.entity.RoleEntity;
import com.pyin.plugin.system.role.repository.RoleRepository;
import com.pyin.plugin.system.user.entity.UserRoleEntity;
import com.pyin.plugin.system.user.model.UserRoleView;
import com.pyin.plugin.system.user.repository.UserRoleRepository;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class UserRoleAssignmentSupport {

    private final UserRoleRepository userRoleRepository;
    private final RoleRepository roleRepository;

    public UserRoleAssignmentSupport(
            UserRoleRepository userRoleRepository,
            RoleRepository roleRepository
    ) {
        this.userRoleRepository = userRoleRepository;
        this.roleRepository = roleRepository;
    }

    public List<Long> sanitizeRoleIds(Collection<Long> roleIds) {
        if (roleIds == null || roleIds.isEmpty()) {
            return List.of();
        }
        return new ArrayList<>(new LinkedHashSet<>(roleIds.stream()
                .filter(Objects::nonNull)
                .toList()));
    }

    public void validateRoleIds(List<Long> roleIds) {
        if (roleIds.isEmpty()) {
            return;
        }
        List<RoleEntity> roles = roleRepository.selectBatchIds(roleIds);
        if (roles.size() != roleIds.size()) {
            Set<Long> existingIds = roles.stream()
                    .map(RoleEntity::getId)
                    .collect(java.util.stream.Collectors.toSet());
            List<Long> missingIds = roleIds.stream()
                    .filter(roleId -> !existingIds.contains(roleId))
                    .toList();
            throw new IllegalArgumentException("角色不存在: " + missingIds);
        }
    }

    public void replaceUserRoles(Long userId, List<Long> roleIds) {
        userRoleRepository.delete(new LambdaQueryWrapper<UserRoleEntity>()
                .eq(UserRoleEntity::getUserId, userId));
        for (Long roleId : roleIds) {
            UserRoleEntity relation = new UserRoleEntity();
            relation.setUserId(userId);
            relation.setRoleId(roleId);
            userRoleRepository.insert(relation);
        }
    }

    public void deleteUserRoles(Long userId) {
        userRoleRepository.delete(new LambdaQueryWrapper<UserRoleEntity>()
                .eq(UserRoleEntity::getUserId, userId));
    }

    public Map<Long, List<UserRoleView>> buildRoleViewMap(List<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Map.of();
        }
        List<UserRoleEntity> relations = userRoleRepository.selectList(new LambdaQueryWrapper<UserRoleEntity>()
                .in(UserRoleEntity::getUserId, userIds));
        if (relations.isEmpty()) {
            return Map.of();
        }

        List<Long> roleIds = relations.stream()
                .map(UserRoleEntity::getRoleId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Map<Long, RoleEntity> roleMap = roleRepository.selectBatchIds(roleIds).stream()
                .collect(java.util.stream.Collectors.toMap(RoleEntity::getId, role -> role));

        Map<Long, List<UserRoleView>> result = new LinkedHashMap<>();
        for (UserRoleEntity relation : relations) {
            RoleEntity role = roleMap.get(relation.getRoleId());
            if (role == null) {
                continue;
            }
            result.computeIfAbsent(relation.getUserId(), key -> new ArrayList<>())
                    .add(new UserRoleView(role.getId(), role.getCode(), role.getName()));
        }
        result.values().forEach(list -> list.sort(java.util.Comparator.comparing(UserRoleView::name)));
        return result;
    }

    public List<Long> findRoleIdsByUserId(Long userId) {
        return userRoleRepository.selectList(new LambdaQueryWrapper<UserRoleEntity>()
                        .eq(UserRoleEntity::getUserId, userId))
                .stream()
                .map(UserRoleEntity::getRoleId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
    }
}
