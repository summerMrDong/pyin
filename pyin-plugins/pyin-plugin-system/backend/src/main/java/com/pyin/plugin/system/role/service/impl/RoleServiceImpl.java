package com.pyin.plugin.system.role.service.impl;

import com.pyin.plugin.system.role.entity.RoleEntity;
import com.pyin.plugin.system.role.model.CreateRoleRequest;
import com.pyin.plugin.system.role.model.RoleDetail;
import com.pyin.plugin.system.role.model.RoleOption;
import com.pyin.plugin.system.role.model.RoleQuery;
import com.pyin.plugin.system.role.model.RoleSummary;
import com.pyin.plugin.system.role.model.UpdateRoleRequest;
import com.pyin.plugin.system.role.repository.RoleRepository;
import com.pyin.plugin.system.role.service.RoleService;
import com.pyin.plugin.system.role.support.RolePermissionBindingSupport;
import com.pyin.plugin.system.role.support.RoleUserBindingSupport;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pyin.plugin.system.resource.repository.RoleResourceRepository;
import com.pyin.plugin.system.resource.entity.RoleResourceEntity;
import com.pyin.plugin.system.user.model.UserRoleView;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class RoleServiceImpl implements RoleService {

    private static final String DEFAULT_ADMIN_ROLE_CODE = "ADMIN";

    private final RoleRepository roleRepository;
    private final RolePermissionBindingSupport rolePermissionBindingSupport;
    private final RoleUserBindingSupport roleUserBindingSupport;
    private final RoleResourceRepository roleResourceRepository;

    public RoleServiceImpl(
            RoleRepository roleRepository,
            RolePermissionBindingSupport rolePermissionBindingSupport,
            RoleUserBindingSupport roleUserBindingSupport,
            RoleResourceRepository roleResourceRepository
    ) {
        this.roleRepository = roleRepository;
        this.rolePermissionBindingSupport = rolePermissionBindingSupport;
        this.roleUserBindingSupport = roleUserBindingSupport;
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
        Map<Long, List<String>> permissionMap = rolePermissionBindingSupport.buildPermissionMap(
                roles.stream().map(RoleEntity::getId).toList());
        Map<Long, Integer> userCountMap = roleUserBindingSupport.buildUserCountMap(
                roles.stream().map(RoleEntity::getId).toList());
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
        int userCount = roleUserBindingSupport.buildUserCountMap(List.of(roleId)).getOrDefault(roleId, 0);
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
        return rolePermissionBindingSupport.findPermissionCodes(roleId);
    }

    @Override
    public List<UserRoleView> findAssignedUsers(Long roleId) {
        requireRole(roleId);
        return roleUserBindingSupport.findAssignedUsers(roleId);
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
        rolePermissionBindingSupport.replacePermissions(
                entity.getId(),
                request == null || request.getPermissionCodes() == null ? List.of() : request.getPermissionCodes()
        );
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
            rolePermissionBindingSupport.replacePermissions(roleId, request.getPermissionCodes());
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
        int userCount = roleUserBindingSupport.buildUserCountMap(List.of(roleId)).getOrDefault(roleId, 0);
        if (userCount > 0) {
            throw new IllegalStateException("角色仍绑定用户，请先解绑后再删除");
        }
        rolePermissionBindingSupport.deletePermissions(roleId);
        roleResourceRepository.delete(new LambdaQueryWrapper<RoleResourceEntity>()
                .eq(RoleResourceEntity::getRoleId, roleId));
        roleRepository.deleteById(roleId);
    }

    @Override
    @Transactional
    public void replacePermissions(Long roleId, List<String> permissionCodes) {
        rolePermissionBindingSupport.replacePermissions(roleId, permissionCodes);
    }

    @Override
    @Transactional
    public void replaceUsers(Long roleId, List<Long> userIds) {
        requireRole(roleId);
        roleUserBindingSupport.replaceUsers(roleId, userIds);
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

}
