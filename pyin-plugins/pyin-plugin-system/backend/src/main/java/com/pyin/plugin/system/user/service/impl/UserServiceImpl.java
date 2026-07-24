package com.pyin.plugin.system.user.service.impl;

import com.pyin.plugin.system.user.entity.UserEntity;
import com.pyin.plugin.system.user.model.CreateUserRequest;
import com.pyin.plugin.system.user.model.ResetUserPasswordRequest;
import com.pyin.plugin.system.user.model.UpdateUserRequest;
import com.pyin.plugin.system.user.model.UserDetail;
import com.pyin.plugin.system.user.model.UserQuery;
import com.pyin.plugin.system.user.model.UserRoleView;
import com.pyin.plugin.system.user.model.UserSummary;
import com.pyin.plugin.system.user.repository.UserRepository;
import com.pyin.plugin.system.user.service.UserService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pyin.plugin.system.user.support.UserRoleAssignmentSupport;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class UserServiceImpl implements UserService {

    private static final String DEFAULT_ADMIN_USERNAME = "admin";

    private final UserRepository userRepository;
    private final UserRoleAssignmentSupport userRoleAssignmentSupport;
    private final PasswordEncoder passwordEncoder;

    public UserServiceImpl(
            UserRepository userRepository,
            UserRoleAssignmentSupport userRoleAssignmentSupport,
            PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.userRoleAssignmentSupport = userRoleAssignmentSupport;
        this.passwordEncoder = passwordEncoder;
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
        Map<Long, List<UserRoleView>> roleMap = userRoleAssignmentSupport.buildRoleViewMap(
                users.stream().map(UserEntity::getId).toList());
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
        List<Long> roleIds = userRoleAssignmentSupport.findRoleIdsByUserId(id);
        Map<Long, List<UserRoleView>> roleMap = userRoleAssignmentSupport.buildRoleViewMap(List.of(id));
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
        List<Long> roleIds = userRoleAssignmentSupport.sanitizeRoleIds(request.getRoleIds());
        userRoleAssignmentSupport.validateRoleIds(roleIds);

        LocalDateTime now = LocalDateTime.now();
        UserEntity entity = new UserEntity();
        entity.setUsername(username);
        entity.setDisplayName(trimToNull(request.getDisplayName()));
        entity.setPasswordHash(passwordEncoder.encode(password));
        entity.setStatus(normalizeStatus(request.getStatus()));
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        userRepository.insert(entity);
        userRoleAssignmentSupport.replaceUserRoles(entity.getId(), roleIds);
        return entity;
    }

    @Override
    @Transactional
    public UserEntity update(Long id, UpdateUserRequest request) {
        UserEntity entity = requireUser(id);
        List<Long> roleIds = userRoleAssignmentSupport.sanitizeRoleIds(request == null ? null : request.getRoleIds());
        userRoleAssignmentSupport.validateRoleIds(roleIds);

        entity.setDisplayName(trimToNull(request.getDisplayName()));
        entity.setStatus(normalizeStatus(request.getStatus()));
        entity.setUpdatedAt(LocalDateTime.now());
        userRepository.updateById(entity);
        userRoleAssignmentSupport.replaceUserRoles(id, roleIds);
        return entity;
    }

    @Override
    @Transactional
    public void resetPassword(Long id, ResetUserPasswordRequest request) {
        UserEntity entity = requireUser(id);
        String newPassword = requireText(request == null ? null : request.getNewPassword(), "新密码不能为空");
        entity.setPasswordHash(passwordEncoder.encode(newPassword));
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
        userRoleAssignmentSupport.deleteUserRoles(id);
        userRepository.deleteById(id);
    }

    private UserEntity requireUser(Long id) {
        UserEntity entity = userRepository.selectById(id);
        if (entity == null) {
            throw new IllegalArgumentException("用户不存在: " + id);
        }
        return entity;
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
