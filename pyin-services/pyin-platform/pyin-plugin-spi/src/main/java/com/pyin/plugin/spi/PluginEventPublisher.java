package com.pyin.plugin.spi;

import com.pyin.plugin.spi.model.PluginEvent;

public interface PluginEventPublisher {

    void publish(PluginEvent event);
}
