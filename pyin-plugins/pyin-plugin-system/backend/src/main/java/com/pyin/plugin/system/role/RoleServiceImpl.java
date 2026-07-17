package com.pyin.plugin.system.role;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pyin.plugin.system.role.resource.RoleResourceRepository;
import com.pyin.plugin.system.role.resource.RoleResourceEntity;
import com.pyin.plugin.system.user.UserEntity;
import com.pyin.plugin.system.user.UserRoleEntity;
import com.pyin.plugin.system.user.UserRoleRepository;
import com.pyin.plugin.system.user.UserRepository;
import com.pyin.plugin.system.user.UserRoleView;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
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
public class RoleServiceImpl implements RoleService {

    private static final String DEFAULT_ADMIN_ROLE_CODE = "ADMIN";

    private final RoleRepository roleRepository;
    private final RolePermissionRepository rolePermissionRepository;
    private final UserRoleRepository userRoleRepository;
    private final UserRepository userRepository;
    private final RoleResourceRepository roleResourceRepository;

    public RoleServiceImpl(
            RoleRepository roleRepository,
            RolePermissionRepository rolePermissionRepository,
            UserRoleRepository userRoleRepository,
            UserRepository userRepository,
            RoleResourceRepository roleResourceRepository
    ) {
        this.roleRepository = roleRepository;
        this.rolePermissionRepository = rolePermissionRepository;
        this.userRoleRepository = userRoleRepository;
        this.userRepository = userRepository;
        this.roleResourceRepository = roleResourceRepository;
    }

    @Override
    public List<RoleSummary> findAll(RoleQuery query) {
        LambdaQueryWrapper<RoleEntity> wrapper = new LambdaQueryWrapper<RoleEntity>()
                .orderByAsc(RoleEntity::getSort)
                .orderByDesc(RoleEntity::getCreatedAt)
                .orderByAsc(RoleEntity::getId);
        if (query != null) {
            if (StringUtils.hasText(query.code())) {
                wrapper.like(RoleEntity::getCode, query.code().trim());
            }
            if (StringUtils.hasText(query.name())) {
                wrapper.like(RoleEntity::getName, query.name().trim());
            }
        }
        List<RoleEntity> roles = roleRepository.selectList(wrapper);
        Map<Long, List<String>> permissionMap = buildPermissionMap(roles.stream().map(RoleEntity::getId).toList());
        Map<Long, Integer> userCountMap = buildUserCountMap(roles.stream().map(RoleEntity::getId).toList());
        return roles.stream()
                .map(role -> {
                    List<String> permissionCodes = permissionMap.getOrDefault(role.getId(), List.of());
                    return new RoleSummary(
                            role.getId(),
                            role.getCode(),
                            role.getName(),
                            role.getDescription(),
                            defaultSort(role.getSort()),
                            permissionCodes,
                            permissionCodes.size(),
                            userCountMap.getOrDefault(role.getId(), 0),
                            role.getCreatedAt()
                    );
                })
                .toList();
    }

    @Override
    public List<RoleOption> findOptions() {
        return roleRepository.selectList(new LambdaQueryWrapper<RoleEntity>()
                        .orderByAsc(RoleEntity::getSort)
                        .orderByAsc(RoleEntity::getCode)
                        .orderByAsc(RoleEntity::getId))
                .stream()
                .map(role -> new RoleOption(role.getId(), role.getCode(), role.getName(), defaultSort(role.getSort())))
                .toList();
    }

    @Override
    public RoleDetail findDetail(Long roleId) {
        RoleEntity role = roleRepository.selectById(roleId);
        if (role == null) {
            return null;
        }
        List<String> permissionCodes = findPermissionCodes(roleId);
        int userCount = buildUserCountMap(List.of(roleId)).getOrDefault(roleId, 0);
        return new RoleDetail(
                role.getId(),
                role.getCode(),
                role.getName(),
                role.getDescription(),
                defaultSort(role.getSort()),
                permissionCodes,
                permissionCodes.size(),
                userCount,
                role.getCreatedAt(),
                role.getUpdatedAt()
        );
    }

    @Override
    public List<String> findPermissionCodes(Long roleId) {
        return rolePermissionRepository.selectList(new LambdaQueryWrapper<RolePermissionEntity>()
                        .eq(RolePermissionEntity::getRoleId, roleId))
                .stream()
                .map(RolePermissionEntity::getPermissionCode)
                .filter(code -> code != null && !code.isBlank())
                .distinct()
                .toList();
    }

    @Override
    public List<UserRoleView> findAssignedUsers(Long roleId) {
        requireRole(roleId);
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

    @Override
    public boolean exists(Long roleId) {
        return roleId != null && roleRepository.selectById(roleId) != null;
    }

    @Override
    public boolean existsAll(List<Long> roleIds) {
        if (roleIds == null || roleIds.isEmpty()) {
            return true;
        }
        return roleRepository.selectBatchIds(roleIds).size() == roleIds.size();
    }

    @Override
    @Transactional
    public RoleEntity create(CreateRoleRequest request) {
        String code = requireText(request == null ? null : request.getCode(), "角色编码不能为空").toUpperCase();
        if (findByCode(code) != null) {
            throw new IllegalArgumentException("角色编码已存在: " + code);
        }
        RoleEntity entity = new RoleEntity();
        entity.setCode(code);
        entity.setName(requireText(request.getName(), "角色名称不能为空"));
        entity.setDescription(trimToNull(request == null ? null : request.getDescription()));
        entity.setSort(normalizeSort(request == null ? null : request.getSort()));
        entity.setCreatedAt(LocalDateTime.now());
        entity.setUpdatedAt(entity.getCreatedAt());
        roleRepository.insert(entity);
        replacePermissions(entity.getId(), request == null || request.getPermissionCodes() == null ? List.of() : request.getPermissionCodes());
        return entity;
    }

    @Override
    @Transactional
    public RoleEntity update(Long roleId, UpdateRoleRequest request) {
        RoleEntity entity = requireRole(roleId);
        entity.setName(requireText(request == null ? null : request.getName(), "角色名称不能为空"));
        entity.setDescription(trimToNull(request == null ? null : request.getDescription()));
        entity.setSort(normalizeSort(request == null ? null : request.getSort()));
        entity.setUpdatedAt(LocalDateTime.now());
        roleRepository.updateById(entity);
        if (request != null && request.getPermissionCodes() != null) {
            replacePermissions(roleId, request.getPermissionCodes());
        }
        return entity;
    }

    @Override
    @Transactional
    public void delete(Long roleId) {
        RoleEntity entity = requireRole(roleId);
        if (DEFAULT_ADMIN_ROLE_CODE.equals(entity.getCode())) {
            throw new IllegalStateException("默认管理员角色不可删除");
        }
        int userCount = buildUserCountMap(List.of(roleId)).getOrDefault(roleId, 0);
        if (userCount > 0) {
            throw new IllegalStateException("角色仍绑定用户，请先解绑后再删除");
        }
        rolePermissionRepository.delete(new LambdaQueryWrapper<RolePermissionEntity>()
                .eq(RolePermissionEntity::getRoleId, roleId));
        roleResourceRepository.delete(new LambdaQueryWrapper<RoleResourceEntity>()
                .eq(RoleResourceEntity::getRoleId, roleId));
        roleRepository.deleteById(roleId);
    }

    @Override
    @Transactional
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

    @Override
    @Transactional
    public void replaceUsers(Long roleId, List<Long> userIds) {
        requireRole(roleId);
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

    private RoleEntity findByCode(String code) {
        return roleRepository.selectOne(new LambdaQueryWrapper<RoleEntity>()
                .eq(RoleEntity::getCode, code)
                .last("LIMIT 1"));
    }

    private RoleEntity requireRole(Long roleId) {
        RoleEntity entity = roleRepository.selectById(roleId);
        if (entity == null) {
            throw new IllegalArgumentException("角色不存在: " + roleId);
        }
        return entity;
    }

    private Map<Long, List<String>> buildPermissionMap(List<Long> roleIds) {
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

    private Map<Long, Integer> buildUserCountMap(List<Long> roleIds) {
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

    private String requireText(String value, String message) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private Integer normalizeSort(Integer value) {
        return value == null ? 0 : value;
    }

    private Integer defaultSort(Integer value) {
        return value == null ? 0 : value;
    }

    private String defaultDisplayName(UserEntity user) {
        return StringUtils.hasText(user.getDisplayName()) ? user.getDisplayName().trim() : user.getUsername();
    }
}
