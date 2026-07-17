package com.pyin.plugin.sdk.standalone;

import com.pyin.plugin.sdk.manifest.PluginDescriptorAssembler;
import com.pyin.plugin.spi.PyinPlugin;
import com.pyin.plugin.spi.model.PluginNodeRegistration;
import com.pyin.plugin.spi.model.ResolvedPluginDescriptor;
import jakarta.annotation.PreDestroy;
import java.util.concurrent.atomic.AtomicBoolean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@ConditionalOnBean({PyinPlugin.class, StandalonePluginRegistrar.class, StandalonePluginProperties.class})
@ConditionalOnProperty(prefix = "pyin.plugin", name = "center-url")
public class StandalonePluginLifecycle {

    private final ApplicationContext applicationContext;
    private final PyinPlugin pyinPlugin;
    private final PluginDescriptorAssembler pluginDescriptorAssembler;
    private final StandalonePluginRegistrar standalonePluginRegistrar;
    private final StandalonePluginProperties standalonePluginProperties;
    private final AtomicBoolean registered = new AtomicBoolean(false);

    public StandalonePluginLifecycle(
            ApplicationContext applicationContext,
            PyinPlugin pyinPlugin,
            PluginDescriptorAssembler pluginDescriptorAssembler,
            StandalonePluginRegistrar standalonePluginRegistrar,
            StandalonePluginProperties standalonePluginProperties
    ) {
        this.applicationContext = applicationContext;
        this.pyinPlugin = pyinPlugin;
        this.pluginDescriptorAssembler = pluginDescriptorAssembler;
        this.standalonePluginRegistrar = standalonePluginRegistrar;
        this.standalonePluginProperties = standalonePluginProperties;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void registerOnReady() {
        if (!hasCenterUrl()) {
            return;
        }
        ResolvedPluginDescriptor descriptor = pluginDescriptorAssembler.assemble(applicationContext, pyinPlugin);
        PluginNodeRegistration registration = new PluginNodeRegistration();
        registration.setPluginId(descriptor.getPluginId());
        registration.setNodeId(standalonePluginProperties.getResolvedNodeId());
        registration.setBackendBaseUrl(standalonePluginProperties.getResolvedBackendBaseUrl());
        registration.setFrontendBaseUrl(standalonePluginProperties.getResolvedFrontendBaseUrl());
        registration.setHealthUrl(standalonePluginProperties.getResolvedHealthUrl());
        registration.setDescriptor(descriptor);
        standalonePluginRegistrar.register(standalonePluginProperties.getCenterUrl(), registration);
        registered.set(true);
    }

    @Scheduled(fixedDelayString = "${pyin.plugin.heartbeat-interval-ms:15000}")
    public void heartbeat() {
        if (!registered.get() || !hasCenterUrl()) {
            return;
        }
        standalonePluginRegistrar.heartbeat(
                standalonePluginProperties.getCenterUrl(),
                pyinPlugin.pluginId(),
                standalonePluginProperties.getResolvedNodeId()
        );
    }

    @PreDestroy
    public void offline() {
        if (!registered.get() || !hasCenterUrl()) {
            return;
        }
        standalonePluginRegistrar.offline(
                standalonePluginProperties.getCenterUrl(),
                pyinPlugin.pluginId(),
                standalonePluginProperties.getResolvedNodeId()
        );
    }

    private boolean hasCenterUrl() {
        return StringUtils.hasText(standalonePluginProperties.getCenterUrl());
    }
}
