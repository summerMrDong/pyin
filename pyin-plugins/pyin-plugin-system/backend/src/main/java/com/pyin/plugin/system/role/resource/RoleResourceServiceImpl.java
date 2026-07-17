package com.pyin.plugin.system.role.resource;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pyin.plugin.system.plugin.PluginEntity;
import com.pyin.plugin.system.plugin.PluginRepository;
import com.pyin.plugin.system.plugin.PluginResourceEntity;
import com.pyin.plugin.system.plugin.PluginResourceRepository;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class RoleResourceServiceImpl implements RoleResourceService {

    private static final String SYSTEM_SCOPE = "SYSTEM";
    private static final String PLUGIN_SCOPE = "PLUGIN";

    private final SystemResourceCatalog systemResourceCatalog;
    private final PluginResourceRepository pluginResourceRepository;
    private final PluginRepository pluginRepository;
    private final RoleResourceRepository roleResourceRepository;

    public RoleResourceServiceImpl(
            SystemResourceCatalog systemResourceCatalog,
            PluginResourceRepository pluginResourceRepository,
            PluginRepository pluginRepository,
            RoleResourceRepository roleResourceRepository
    ) {
        this.systemResourceCatalog = systemResourceCatalog;
        this.pluginResourceRepository = pluginResourceRepository;
        this.pluginRepository = pluginRepository;
        this.roleResourceRepository = roleResourceRepository;
    }

    @Override
    public ResourceTreeResponse findResourceTree() {
        List<ResourceNode> systemResources = buildSystemResourceTree();
        List<ResourcePluginGroup> pluginGroups = buildPluginResourceGroups();
        return new ResourceTreeResponse(systemResources, pluginGroups);
    }

    @Override
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

    @Override
    @Transactional
    public void replaceRoleResources(Long roleId, List<String> resourceKeys) {
        List<String> sanitizedKeys = sanitizeKeys(resourceKeys);
        Set<String> knownKeys = collectAllResourceKeys();
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

    private List<ResourceNode> buildSystemResourceTree() {
        List<ResourceNode> flatNodes = systemResourceCatalog.definitions().stream()
                .map(definition -> new ResourceNode(
                        systemKey(definition.resourceCode()),
                        definition.resourceCode(),
                        definition.resourceName(),
                        definition.resourceType(),
                        SYSTEM_SCOPE,
                        null,
                        definition.parentCode() == null ? null : systemKey(definition.parentCode()),
                        definition.path(),
                        definition.icon(),
                        defaultSort(definition.sort()),
                        definition.permissionCode(),
                        definition.visible(),
                        List.of()
                ))
                .toList();
        return buildTree(flatNodes);
    }

    private List<ResourcePluginGroup> buildPluginResourceGroups() {
        Map<String, String> pluginNameMap = pluginRepository.selectList(null).stream()
                .collect(java.util.stream.Collectors.toMap(
                        PluginEntity::getPluginId,
                        plugin -> StringUtils.hasText(plugin.getPluginName()) ? plugin.getPluginName().trim() : plugin.getPluginId(),
                        (left, right) -> left,
                        LinkedHashMap::new
                ));
        Map<String, List<PluginResourceEntity>> byPlugin = pluginResourceRepository.selectList(
                        new LambdaQueryWrapper<PluginResourceEntity>()
                                .orderByAsc(PluginResourceEntity::getPluginId)
                                .orderByAsc(PluginResourceEntity::getSort)
                                .orderByAsc(PluginResourceEntity::getResourceCode))
                .stream()
                .filter(resource -> StringUtils.hasText(resource.getPluginId()))
                .filter(this::isGrantableResource)
                .collect(java.util.stream.Collectors.groupingBy(
                        PluginResourceEntity::getPluginId,
                        LinkedHashMap::new,
                        java.util.stream.Collectors.toList()
                ));
        List<ResourcePluginGroup> groups = new ArrayList<>();
        for (Map.Entry<String, List<PluginResourceEntity>> entry : byPlugin.entrySet()) {
            String pluginId = entry.getKey();
            List<ResourceNode> flatNodes = entry.getValue().stream()
                    .map(resource -> new ResourceNode(
                            pluginKey(pluginId, resource.getResourceCode()),
                            resource.getResourceCode(),
                            resource.getResourceName(),
                            resource.getResourceType(),
                            PLUGIN_SCOPE,
                            pluginId,
                            StringUtils.hasText(resource.getParentCode()) ? pluginKey(pluginId, resource.getParentCode()) : null,
                            resource.getPath(),
                            resource.getIcon(),
                            defaultSort(resource.getSort()),
                            resource.getPermissionCode(),
                            resource.getVisible(),
                            List.of()
                    ))
                    .toList();
            groups.add(new ResourcePluginGroup(
                    pluginId,
                    pluginNameMap.getOrDefault(pluginId, pluginId),
                    buildTree(flatNodes)
            ));
        }
        return groups;
    }

    private List<ResourceNode> buildTree(List<ResourceNode> flatNodes) {
        Map<String, List<ResourceNode>> childrenMap = flatNodes.stream()
                .filter(node -> StringUtils.hasText(node.parentKey()))
                .collect(java.util.stream.Collectors.groupingBy(
                        ResourceNode::parentKey,
                        LinkedHashMap::new,
                        java.util.stream.Collectors.toList()
                ));
        return flatNodes.stream()
                .filter(node -> !StringUtils.hasText(node.parentKey()))
                .map(node -> withChildren(node, childrenMap))
                .sorted(resourceNodeComparator())
                .toList();
    }

    private ResourceNode withChildren(ResourceNode node, Map<String, List<ResourceNode>> childrenMap) {
        List<ResourceNode> children = childrenMap.getOrDefault(node.resourceKey(), List.of()).stream()
                .map(child -> withChildren(child, childrenMap))
                .sorted(resourceNodeComparator())
                .toList();
        return new ResourceNode(
                node.resourceKey(),
                node.resourceCode(),
                node.resourceName(),
                node.resourceType(),
                node.resourceScope(),
                node.pluginId(),
                node.parentKey(),
                node.path(),
                node.icon(),
                node.sort(),
                node.permissionCode(),
                node.visible(),
                children
        );
    }

    private Comparator<ResourceNode> resourceNodeComparator() {
        return Comparator
                .comparing((ResourceNode node) -> defaultSort(node.sort()))
                .thenComparing(node -> node.resourceName() == null ? "" : node.resourceName())
                .thenComparing(ResourceNode::resourceKey);
    }

    private Set<String> collectAllResourceKeys() {
        Set<String> keys = new LinkedHashSet<>();
        flattenResourceKeys(findResourceTree().systemResources(), keys);
        for (ResourcePluginGroup group : findResourceTree().pluginGroups()) {
            flattenResourceKeys(group.resources(), keys);
        }
        return keys;
    }

    private void flattenResourceKeys(Collection<ResourceNode> nodes, Set<String> keys) {
        for (ResourceNode node : nodes) {
            keys.add(node.resourceKey());
            flattenResourceKeys(node.children(), keys);
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
        return index > 0 ? resourceKey.substring(0, index) : SYSTEM_SCOPE;
    }

    private String extractResourceCode(String resourceKey) {
        int index = resourceKey.indexOf(':');
        return index > -1 ? resourceKey.substring(index + 1) : resourceKey;
    }

    private String systemKey(String resourceCode) {
        return SYSTEM_SCOPE + ":" + resourceCode;
    }

    private String pluginKey(String pluginId, String resourceCode) {
        return PLUGIN_SCOPE + ":" + pluginId + "/" + resourceCode;
    }

    private Integer defaultSort(Integer value) {
        return value == null ? 0 : value;
    }

    private boolean isGrantableResource(PluginResourceEntity resource) {
        if (resource == null || !StringUtils.hasText(resource.getResourceType())) {
            return false;
        }
        String resourceType = resource.getResourceType().trim().toUpperCase();
        return "PAGE".equals(resourceType) || "BUTTON".equals(resourceType);
    }
}
