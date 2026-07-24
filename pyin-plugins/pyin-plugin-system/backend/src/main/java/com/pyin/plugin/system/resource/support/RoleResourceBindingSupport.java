package com.pyin.plugin.system.resource.support;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pyin.plugin.system.resource.entity.RoleResourceEntity;
import com.pyin.plugin.system.resource.repository.RoleResourceRepository;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class RoleResourceBindingSupport {

    private final RoleResourceRepository roleResourceRepository;
    private final ResourceTreeBuilder resourceTreeBuilder;

    public RoleResourceBindingSupport(
            RoleResourceRepository roleResourceRepository,
            ResourceTreeBuilder resourceTreeBuilder
    ) {
        this.roleResourceRepository = roleResourceRepository;
        this.resourceTreeBuilder = resourceTreeBuilder;
    }

    public List<String> findRoleResourceKeys(Long roleId) {
        return roleResourceRepository.selectList(new LambdaQueryWrapper<RoleResourceEntity>()
                        .eq(RoleResourceEntity::getRoleId, roleId))
                .stream()
                .map(this::toResourceKey)
                .filter(StringUtils::hasText)
                .distinct()
                .sorted()
                .toList();
    }

    public void replaceRoleResources(Long roleId, List<String> resourceKeys) {
        List<String> sanitizedKeys = sanitizeKeys(resourceKeys);
        Set<String> knownKeys = resourceTreeBuilder.collectAllResourceKeys();
        List<String> unknownKeys = sanitizedKeys.stream()
                .filter(key -> !knownKeys.contains(key))
                .toList();
        if (!unknownKeys.isEmpty()) {
            throw new IllegalArgumentException("未知资源编码: " + unknownKeys);
        }

        roleResourceRepository.delete(new LambdaQueryWrapper<RoleResourceEntity>()
                .eq(RoleResourceEntity::getRoleId, roleId));
        for (String resourceKey : sanitizedKeys) {
            RoleResourceEntity entity = new RoleResourceEntity();
            entity.setRoleId(roleId);
            entity.setResourceCode(extractResourceCode(resourceKey));
            entity.setResourceScope(extractScope(resourceKey));
            entity.setCreatedAt(LocalDateTime.now());
            roleResourceRepository.insert(entity);
        }
    }

    private List<String> sanitizeKeys(List<String> resourceKeys) {
        if (resourceKeys == null || resourceKeys.isEmpty()) {
            return List.of();
        }
        return new ArrayList<>(new LinkedHashSet<>(resourceKeys.stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .toList()));
    }

    private String toResourceKey(RoleResourceEntity entity) {
        if (!StringUtils.hasText(entity.getResourceScope()) || !StringUtils.hasText(entity.getResourceCode())) {
            return null;
        }
        return entity.getResourceScope().trim() + ":" + entity.getResourceCode().trim();
    }

    private String extractScope(String resourceKey) {
        int index = resourceKey.indexOf(':');
        return index > 0 ? resourceKey.substring(0, index) : ResourceTreeBuilder.SYSTEM_SCOPE;
    }

    private String extractResourceCode(String resourceKey) {
        int index = resourceKey.indexOf(':');
        return index > -1 ? resourceKey.substring(index + 1) : resourceKey;
    }
}
