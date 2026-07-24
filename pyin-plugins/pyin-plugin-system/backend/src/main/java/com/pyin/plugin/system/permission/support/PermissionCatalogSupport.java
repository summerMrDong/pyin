package com.pyin.plugin.system.permission.support;

import com.pyin.plugin.system.permission.model.PermissionSummary;
import com.pyin.plugin.system.permission.repository.PermissionRepository;
import com.pyin.plugin.system.permission.repository.PluginPermissionRepository;
import com.pyin.plugin.system.plugin.entity.PluginEntity;
import com.pyin.plugin.system.plugin.repository.PluginRepository;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class PermissionCatalogSupport {

    private final PermissionRepository permissionRepository;
    private final PluginPermissionRepository pluginPermissionRepository;
    private final PluginRepository pluginRepository;

    public PermissionCatalogSupport(
            PermissionRepository permissionRepository,
            PluginPermissionRepository pluginPermissionRepository,
            PluginRepository pluginRepository
    ) {
        this.permissionRepository = permissionRepository;
        this.pluginPermissionRepository = pluginPermissionRepository;
        this.pluginRepository = pluginRepository;
    }

    public List<PermissionSummary> findAll() {
        java.util.Map<String, String> pluginNameMap = pluginRepository.selectList(null).stream()
                .collect(java.util.stream.Collectors.toMap(
                        PluginEntity::getPluginId,
                        plugin -> plugin.getPluginName() == null || plugin.getPluginName().isBlank()
                                ? plugin.getPluginId()
                                : plugin.getPluginName().trim(),
                        (left, right) -> left,
                        java.util.LinkedHashMap::new
                ));
        List<PermissionSummary> systemPermissions = permissionRepository.selectList(null).stream()
                .map(permission -> new PermissionSummary(
                        permission.getCode(),
                        permission.getName(),
                        "SYSTEM",
                        null,
                        null,
                        "SYSTEM"
                ))
                .toList();

        List<PermissionSummary> pluginPermissions = pluginPermissionRepository.selectList(null).stream()
                .map(permission -> new PermissionSummary(
                        permission.getPermissionCode(),
                        permission.getPermissionName(),
                        "PLUGIN",
                        permission.getPluginId(),
                        pluginNameMap.getOrDefault(permission.getPluginId(), permission.getPluginId()),
                        permission.getResourceType()
                ))
                .toList();

        return java.util.stream.Stream.concat(systemPermissions.stream(), pluginPermissions.stream())
                .toList();
    }
}
