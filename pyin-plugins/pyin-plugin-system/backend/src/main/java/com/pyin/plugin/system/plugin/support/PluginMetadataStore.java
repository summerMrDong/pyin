package com.pyin.plugin.system.plugin.support;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pyin.plugin.spi.model.PluginApiDefinition;
import com.pyin.plugin.spi.model.PluginPermission;
import com.pyin.plugin.spi.model.PluginResourceDefinition;
import com.pyin.plugin.spi.model.ResolvedPluginDescriptor;
import com.pyin.plugin.system.permission.entity.PluginPermissionEntity;
import com.pyin.plugin.system.permission.repository.PluginPermissionRepository;
import com.pyin.plugin.system.plugin.entity.PluginApiEntity;
import com.pyin.plugin.system.plugin.entity.PluginEntity;
import com.pyin.plugin.system.plugin.entity.PluginResourceEntity;
import com.pyin.plugin.system.plugin.repository.PluginApiRepository;
import com.pyin.plugin.system.plugin.repository.PluginRepository;
import com.pyin.plugin.system.plugin.repository.PluginResourceRepository;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class PluginMetadataStore {

    private final PluginRepository pluginRepository;
    private final PluginPermissionRepository pluginPermissionRepository;
    private final PluginApiRepository pluginApiRepository;
    private final PluginResourceRepository pluginResourceRepository;
    private final ObjectMapper objectMapper;

    public PluginMetadataStore(
            PluginRepository pluginRepository,
            PluginPermissionRepository pluginPermissionRepository,
            PluginApiRepository pluginApiRepository,
            PluginResourceRepository pluginResourceRepository,
            ObjectMapper objectMapper
    ) {
        this.pluginRepository = pluginRepository;
        this.pluginPermissionRepository = pluginPermissionRepository;
        this.pluginApiRepository = pluginApiRepository;
        this.pluginResourceRepository = pluginResourceRepository;
        this.objectMapper = objectMapper;
    }

    public void upsertPlugin(ResolvedPluginDescriptor descriptor) {
        PluginEntity entity = pluginRepository.selectById(descriptor.getPluginId());
        boolean exists = entity != null;
        if (!exists) {
            entity = new PluginEntity();
            entity.setPluginId(descriptor.getPluginId());
        }
        entity.setPluginName(descriptor.getPluginName());
        entity.setVersion(descriptor.getPluginVersion());
        if (!exists) {
            pluginRepository.insert(entity);
            return;
        }
        pluginRepository.updateById(entity);
    }

    public void replacePluginCapabilities(ResolvedPluginDescriptor descriptor) {
        String pluginId = descriptor.getPluginId();
        pluginPermissionRepository.delete(new LambdaQueryWrapper<PluginPermissionEntity>()
                .eq(PluginPermissionEntity::getPluginId, pluginId));
        pluginApiRepository.delete(new LambdaQueryWrapper<PluginApiEntity>()
                .eq(PluginApiEntity::getPluginId, pluginId));
        pluginResourceRepository.delete(new LambdaQueryWrapper<PluginResourceEntity>()
                .eq(PluginResourceEntity::getPluginId, pluginId));

        replacePermissions(pluginId, descriptor.getPermissions() == null ? List.of() : descriptor.getPermissions());
        replaceApis(pluginId, descriptor.getApis() == null ? List.of() : descriptor.getApis());
        replaceResources(pluginId, descriptor.getResources() == null ? List.of() : descriptor.getResources());
    }

    private void replacePermissions(String pluginId, List<PluginPermission> permissions) {
        for (PluginPermission permission : permissions) {
            PluginPermissionEntity entity = new PluginPermissionEntity();
            entity.setPluginId(pluginId);
            entity.setPermissionCode(permission.code());
            entity.setPermissionName(permission.name());
            entity.setResourceType(permission.resourceType().name());
            pluginPermissionRepository.insert(entity);
        }
    }

    private void replaceApis(String pluginId, List<PluginApiDefinition> apis) {
        for (PluginApiDefinition apiDefinition : apis) {
            PluginApiEntity entity = new PluginApiEntity();
            entity.setPluginId(pluginId);
            entity.setPath(apiDefinition.path());
            entity.setMethod(apiDefinition.method());
            entity.setAccessMode(apiDefinition.accessMode().name());
            entity.setPermissionCode(apiDefinition.permissionCode());
            entity.setAuditEnabled(apiDefinition.auditEnabled());
            pluginApiRepository.insert(entity);
        }
    }

    private void replaceResources(String pluginId, List<PluginResourceDefinition> resources) {
        for (PluginResourceDefinition resource : resources) {
            PluginResourceEntity entity = new PluginResourceEntity();
            entity.setPluginId(pluginId);
            entity.setResourceCode(resource.resourceCode());
            entity.setResourceName(resource.resourceName());
            entity.setResourceType(resource.resourceType().name());
            entity.setParentCode(resource.parentCode());
            entity.setPath(resource.path());
            entity.setIcon(resource.icon());
            entity.setSort(resource.sort());
            entity.setPermissionCode(resource.permissionCode());
            entity.setVisible(resource.visible());
            entity.setMetadataJson(toJson(resource.metadata()));
            pluginResourceRepository.insert(entity);
        }
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to serialize plugin resource metadata", exception);
        }
    }
}
