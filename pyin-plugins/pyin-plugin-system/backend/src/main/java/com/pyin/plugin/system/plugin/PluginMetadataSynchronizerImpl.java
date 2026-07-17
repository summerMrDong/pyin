package com.pyin.plugin.system.plugin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pyin.plugin.system.permission.PluginPermissionEntity;
import com.pyin.plugin.system.permission.PluginPermissionRepository;
import com.pyin.plugin.spi.PluginMetadataSynchronizer;
import com.pyin.plugin.spi.model.PluginResourceDefinition;
import com.pyin.plugin.spi.model.PluginApiDefinition;
import com.pyin.plugin.spi.model.PluginPermission;
import com.pyin.plugin.spi.model.ResolvedPluginDescriptor;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PluginMetadataSynchronizerImpl implements PluginMetadataSynchronizer {

    private final PluginRepository pluginRepository;
    private final PluginPermissionRepository pluginPermissionRepository;
    private final PluginApiRepository pluginApiRepository;
    private final PluginResourceRepository pluginResourceRepository;
    private final CompiledPluginApiRegistry compiledPluginApiRegistry;
    private final ObjectMapper objectMapper;

    public PluginMetadataSynchronizerImpl(
            PluginRepository pluginRepository,
            PluginPermissionRepository pluginPermissionRepository,
            PluginApiRepository pluginApiRepository,
            PluginResourceRepository pluginResourceRepository,
            CompiledPluginApiRegistry compiledPluginApiRegistry,
            ObjectMapper objectMapper
    ) {
        this.pluginRepository = pluginRepository;
        this.pluginPermissionRepository = pluginPermissionRepository;
        this.pluginApiRepository = pluginApiRepository;
        this.pluginResourceRepository = pluginResourceRepository;
        this.compiledPluginApiRegistry = compiledPluginApiRegistry;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public void sync(ResolvedPluginDescriptor descriptor) {
        if (descriptor == null || descriptor.getPluginId() == null || descriptor.getPluginId().isBlank()) {
            return;
        }
        String pluginId = descriptor.getPluginId();
        upsertPlugin(descriptor);
        CompiledPluginApiRegistry.CompiledRegistrySnapshot compiledSnapshot = compiledPluginApiRegistry.compile(descriptor);
        pluginPermissionRepository.delete(new LambdaQueryWrapper<PluginPermissionEntity>()
                .eq(PluginPermissionEntity::getPluginId, pluginId));
        pluginApiRepository.delete(new LambdaQueryWrapper<PluginApiEntity>()
                .eq(PluginApiEntity::getPluginId, pluginId));
        pluginResourceRepository.delete(new LambdaQueryWrapper<PluginResourceEntity>()
                .eq(PluginResourceEntity::getPluginId, pluginId));

        List<PluginPermission> permissions = descriptor.getPermissions() == null ? List.of() : descriptor.getPermissions();
        for (PluginPermission permission : permissions) {
            PluginPermissionEntity entity = new PluginPermissionEntity();
            entity.setPluginId(pluginId);
            entity.setPermissionCode(permission.code());
            entity.setPermissionName(permission.name());
            entity.setResourceType(permission.resourceType().name());
            pluginPermissionRepository.insert(entity);
        }

        List<PluginApiDefinition> apis = descriptor.getApis() == null ? List.of() : descriptor.getApis();
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
        List<PluginResourceDefinition> resources = descriptor.getResources() == null ? List.of() : descriptor.getResources();
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
        compiledPluginApiRegistry.replace(compiledSnapshot);
    }

    private void upsertPlugin(ResolvedPluginDescriptor descriptor) {
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

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to serialize plugin resource metadata", exception);
        }
    }
}
