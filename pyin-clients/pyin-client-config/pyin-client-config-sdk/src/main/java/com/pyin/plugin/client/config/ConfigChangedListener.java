package com.pyin.plugin.client.config;

import com.pyin.plugin.client.config.event.ConfigChangedEvent;

public interface ConfigChangedListener {

    void onChanged(ConfigChangedEvent event);
}
