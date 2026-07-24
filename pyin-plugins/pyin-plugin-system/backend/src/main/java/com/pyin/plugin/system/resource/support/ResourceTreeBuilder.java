package com.pyin.plugin.system.resource.support;

import com.pyin.plugin.system.plugin.entity.PluginEntity;
import com.pyin.plugin.system.plugin.entity.PluginResourceEntity;
import com.pyin.plugin.system.plugin.repository.PluginRepository;
import com.pyin.plugin.system.plugin.repository.PluginResourceRepository;
import com.pyin.plugin.system.resource.model.ResourceNode;
import com.pyin.plugin.system.resource.model.ResourcePluginGroup;
import com.pyin.plugin.system.resource.model.ResourceTreeResponse;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class ResourceTreeBuilder {

    static final String SYSTEM_SCOPE = "SYSTEM";
    static final String PLUGIN_SCOPE = "PLUGIN";

    private final SystemResourceCatalog systemResourceCatalog;
    private final PluginResourceRepository pluginResourceRepository;
    private final PluginRepository pluginRepository;

    public ResourceTreeBuilder(
            SystemResourceCatalog systemResourceCatalog,
            PluginResourceRepository pluginResourceRepository,
            PluginRepository pluginRepository
    ) {
        this.systemResourceCatalog = systemResourceCatalog;
        this.pluginResourceRepository = pluginResourceRepository;
        this.pluginRepository = pluginRepository;
    }

    public ResourceTreeResponse buildResourceTree() {
        return new ResourceTreeResponse(buildSystemResourceTree(), buildPluginResourceGroups());
    }

    public Set<String> collectAllResourceKeys() {
        ResourceTreeResponse tree = buildResourceTree();
        Set<String> keys = new LinkedHashSet<>();
        flattenResourceKeys(tree.systemResources(), keys);
        for (ResourcePluginGroup group : tree.pluginGroups()) {
            flattenResourceKeys(group.resources(), keys);
        }
        return keys;
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
                        new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<PluginResourceEntity>()
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

    private void flattenResourceKeys(Collection<ResourceNode> nodes, Set<String> keys) {
        for (ResourceNode node : nodes) {
            keys.add(node.resourceKey());
            flattenResourceKeys(node.children(), keys);
        }
    }

    private Comparator<ResourceNode> resourceNodeComparator() {
        return Comparator
                .comparing((ResourceNode node) -> defaultSort(node.sort()))
                .thenComparing(node -> node.resourceName() == null ? "" : node.resourceName())
                .thenComparing(ResourceNode::resourceKey);
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
