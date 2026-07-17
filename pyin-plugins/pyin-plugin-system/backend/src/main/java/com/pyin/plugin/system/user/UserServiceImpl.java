package com.pyin.plugin.system.user;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pyin.center.auth.crypto.PasswordHasher;
import com.pyin.plugin.system.role.RoleEntity;
import com.pyin.plugin.system.role.RoleRepository;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class UserServiceImpl implements UserService {

    private static final String DEFAULT_ADMIN_USERNAME = "admin";

    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;
    private final RoleRepository roleRepository;
    private final PasswordHasher passwordHasher;

    public UserServiceImpl(
            UserRepository userRepository,
            UserRoleRepository userRoleRepository,
            RoleRepository roleRepository,
            PasswordHasher passwordHasher
    ) {
        this.userRepository = userRepository;
        this.userRoleRepository = userRoleRepository;
        this.roleRepository = roleRepository;
        this.passwordHasher = passwordHasher;
    }

    @Override
    public List<UserSummary> findAll(UserQuery query) {
        LambdaQueryWrapper<UserEntity> wrapper = new LambdaQueryWrapper<UserEntity>()
                .orderByDesc(UserEntity::getCreatedAt)
                .orderByAsc(UserEntity::getId);
        if (query != null) {
            if (StringUtils.hasText(query.username())) {
                wrapper.like(UserEntity::getUsername, query.username().trim());
            }
            if (StringUtils.hasText(query.displayName())) {
                wrapper.like(UserEntity::getDisplayName, query.displayName().trim());
            }
            if (StringUtils.hasText(query.status())) {
                wrapper.eq(UserEntity::getStatus, normalizeStatus(query.status()));
            }
        }

        List<UserEntity> users = userRepository.selectList(wrapper);
        Map<Long, List<UserRoleView>> roleMap = buildRoleViewMap(users.stream().map(UserEntity::getId).toList());
        return users.stream()
                .map(user -> new UserSummary(
                        user.getId(),
                        user.getUsername(),
                        user.getDisplayName(),
                        defaultStatus(user.getStatus()),
                        user.getCreatedAt(),
                        roleMap.getOrDefault(user.getId(), List.of())
                ))
                .toList();
    }

    @Override
    public UserDetail findDetail(Long id) {
        UserEntity user = userRepository.selectById(id);
        if (user == null) {
            return null;
        }
        List<Long> roleIds = findRoleIdsByUserId(id);
        Map<Long, List<UserRoleView>> roleMap = buildRoleViewMap(List.of(id));
        return new UserDetail(
                user.getId(),
                user.getUsername(),
                user.getDisplayName(),
                defaultStatus(user.getStatus()),
                user.getCreatedAt(),
                user.getUpdatedAt(),
                roleIds,
                roleMap.getOrDefault(id, List.of())
        );
    }

    @Override
    public UserEntity findById(Long id) {
        return userRepository.selectById(id);
    }

    @Override
    public UserEntity findByUsername(String username) {
        return userRepository.selectOne(new LambdaQueryWrapper<UserEntity>()
                .eq(UserEntity::getUsername, username)
                .last("LIMIT 1"));
    }

    @Override
    @Transactional
    public UserEntity create(CreateUserRequest request) {
        String username = requireText(request == null ? null : request.getUsername(), "用户名不能为空");
        if (findByUsername(username) != null) {
            throw new IllegalArgumentException("用户名已存在: " + username);
        }
        String password = requireText(request.getPassword(), "初始密码不能为空");
        List<Long> roleIds = sanitizeRoleIds(request.getRoleIds());
        validateRoleIds(roleIds);

        LocalDateTime now = LocalDateTime.now();
        UserEntity entity = new UserEntity();
        entity.setUsername(username);
        entity.setDisplayName(trimToNull(request.getDisplayName()));
        entity.setPasswordHash(passwordHasher.hash(password));
        entity.setStatus(normalizeStatus(request.getStatus()));
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        userRepository.insert(entity);
        replaceUserRoles(entity.getId(), roleIds);
        return entity;
    }

    @Override
    @Transactional
    public UserEntity update(Long id, UpdateUserRequest request) {
        UserEntity entity = requireUser(id);
        List<Long> roleIds = sanitizeRoleIds(request == null ? null : request.getRoleIds());
        validateRoleIds(roleIds);

        entity.setDisplayName(trimToNull(request.getDisplayName()));
        entity.setStatus(normalizeStatus(request.getStatus()));
        entity.setUpdatedAt(LocalDateTime.now());
        userRepository.updateById(entity);
        replaceUserRoles(id, roleIds);
        return entity;
    }

    @Override
    @Transactional
    public void resetPassword(Long id, ResetUserPasswordRequest request) {
        UserEntity entity = requireUser(id);
        String newPassword = requireText(request == null ? null : request.getNewPassword(), "新密码不能为空");
        entity.setPasswordHash(passwordHasher.hash(newPassword));
        entity.setUpdatedAt(LocalDateTime.now());
        userRepository.updateById(entity);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        UserEntity entity = requireUser(id);
        if (DEFAULT_ADMIN_USERNAME.equals(entity.getUsername())) {
            throw new IllegalStateException("默认管理员账号不可删除");
        }
        userRoleRepository.delete(new LambdaQueryWrapper<UserRoleEntity>()
                .eq(UserRoleEntity::getUserId, id));
        userRepository.deleteById(id);
    }

    private UserEntity requireUser(Long id) {
        UserEntity entity = userRepository.selectById(id);
        if (entity == null) {
            throw new IllegalArgumentException("用户不存在: " + id);
        }
        return entity;
    }

    private void validateRoleIds(List<Long> roleIds) {
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

    private void replaceUserRoles(Long userId, List<Long> roleIds) {
        userRoleRepository.delete(new LambdaQueryWrapper<UserRoleEntity>()
                .eq(UserRoleEntity::getUserId, userId));
        for (Long roleId : roleIds) {
            UserRoleEntity relation = new UserRoleEntity();
            relation.setUserId(userId);
            relation.setRoleId(roleId);
            userRoleRepository.insert(relation);
        }
    }

    private Map<Long, List<UserRoleView>> buildRoleViewMap(List<Long> userIds) {
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

    private List<Long> findRoleIdsByUserId(Long userId) {
        return userRoleRepository.selectList(new LambdaQueryWrapper<UserRoleEntity>()
                        .eq(UserRoleEntity::getUserId, userId))
                .stream()
                .map(UserRoleEntity::getRoleId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
    }

    private List<Long> sanitizeRoleIds(Collection<Long> roleIds) {
        if (roleIds == null || roleIds.isEmpty()) {
            return List.of();
        }
        return new ArrayList<>(new LinkedHashSet<>(roleIds.stream()
                .filter(Objects::nonNull)
                .toList()));
    }

    private String requireText(String value, String message) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }

    private String normalizeStatus(String value) {
        if (!StringUtils.hasText(value)) {
            return STATUS_ENABLED;
        }
        String normalized = value.trim().toUpperCase();
        if (!STATUS_ENABLED.equals(normalized) && !STATUS_DISABLED.equals(normalized)) {
            throw new IllegalArgumentException("不支持的用户状态: " + value);
        }
        return normalized;
    }

    private String defaultStatus(String value) {
        return StringUtils.hasText(value) ? value : STATUS_ENABLED;
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
