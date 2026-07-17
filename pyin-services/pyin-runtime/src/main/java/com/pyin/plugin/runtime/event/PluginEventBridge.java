package com.pyin.plugin.runtime.event;

import com.pyin.plugin.spi.PluginEventPublisher;
import com.pyin.plugin.spi.model.PluginEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
public class  PluginEventBridge implements PluginEventPublisher {

    private final ApplicationEventPublisher applicationEventPublisher;

    public PluginEventBridge(ApplicationEventPublisher applicationEventPublisher) {
        this.applicationEventPublisher = applicationEventPublisher;
    }

    @Override
    public void publish(PluginEvent event) {
        applicationEventPublisher.publishEvent(event);
    }
}
