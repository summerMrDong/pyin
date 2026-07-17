package com.pyin.plugin.sdk.event;

import com.pyin.plugin.spi.PluginEventPublisher;
import com.pyin.plugin.spi.model.PluginEvent;

public class PluginEventSupport {

    private final PluginEventPublisher eventPublisher;

    public PluginEventSupport(PluginEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    public void publish(PluginEvent event) {
        eventPublisher.publish(event);
    }
}
