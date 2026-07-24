package com.pyin.plugin.system.role.support;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pyin.plugin.system.user.entity.UserEntity;
import com.pyin.plugin.system.user.entity.UserRoleEntity;
import com.pyin.plugin.system.user.model.UserRoleView;
import com.pyin.plugin.system.user.repository.UserRepository;
import com.pyin.plugin.system.user.repository.UserRoleRepository;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class RoleUserBindingSupport {

    private final UserRoleRepository userRoleRepository;
    private final UserRepository userRepository;

    public RoleUserBindingSupport(
            UserRoleRepository userRoleRepository,
            UserRepository userRepository
    ) {
        this.userRoleRepository = userRoleRepository;
        this.userRepository = userRepository;
    }

    public List<UserRoleView> findAssignedUsers(Long roleId) {
        List<Long> userIds = userRoleRepository.selectList(new LambdaQueryWrapper<UserRoleEntity>()
                        .eq(UserRoleEntity::getRoleId, roleId))
                .stream()
                .map(UserRoleEntity::getUserId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (userIds.isEmpty()) {
            return List.of();
        }
        return userRepository.selectBatchIds(userIds).stream()
                .sorted(Comparator
                        .comparing((UserEntity user) -> user.getDisplayName() == null ? "" : user.getDisplayName())
                        .thenComparing(UserEntity::getUsername))
                .map(user -> new UserRoleView(user.getId(), user.getUsername(), defaultDisplayName(user)))
                .toList();
    }

    public Map<Long, Integer> buildUserCountMap(List<Long> roleIds) {
        if (roleIds == null || roleIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, Integer> result = new LinkedHashMap<>();
        for (UserRoleEntity relation : userRoleRepository.selectList(new LambdaQueryWrapper<UserRoleEntity>()
                .in(UserRoleEntity::getRoleId, roleIds))) {
            result.merge(relation.getRoleId(), 1, Integer::sum);
        }
        return result;
    }

    public void replaceUsers(Long roleId, List<Long> userIds) {
        List<Long> sanitizedUserIds = sanitizeUserIds(userIds);
        validateUserIds(sanitizedUserIds);

        userRoleRepository.delete(new LambdaQueryWrapper<UserRoleEntity>()
                .eq(UserRoleEntity::getRoleId, roleId));
        for (Long userId : sanitizedUserIds) {
            UserRoleEntity relation = new UserRoleEntity();
            relation.setUserId(userId);
            relation.setRoleId(roleId);
            userRoleRepository.insert(relation);
        }
    }

    private List<Long> sanitizeUserIds(List<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return List.of();
        }
        return new ArrayList<>(new LinkedHashSet<>(userIds.stream()
                .filter(Objects::nonNull)
                .toList()));
    }

    private void validateUserIds(List<Long> userIds) {
        if (userIds.isEmpty()) {
            return;
        }
        List<UserEntity> users = userRepository.selectBatchIds(userIds);
        if (users.size() != userIds.size()) {
            Set<Long> existingIds = users.stream()
                    .map(UserEntity::getId)
                    .collect(java.util.stream.Collectors.toSet());
            List<Long> missingIds = userIds.stream()
                    .filter(id -> !existingIds.contains(id))
                    .toList();
            throw new IllegalArgumentException("用户不存在: " + missingIds);
        }
    }

    private String defaultDisplayName(UserEntity user) {
        return StringUtils.hasText(user.getDisplayName()) ? user.getDisplayName().trim() : user.getUsername();
    }
}
