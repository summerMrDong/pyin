package com.pyin.plugin.client.api;

import com.pyin.plugin.client.api.http.PyinCenterHttpClient;

public interface PyinClientFeatureContext {

    PyinCenterHttpClient httpClient();

    Object properties();

    void publishLocalEvent(Object event);
}
