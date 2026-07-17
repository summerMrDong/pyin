package com.pyin.plugin.client.core.context;

import com.pyin.plugin.client.api.PyinClientFeatureContext;
import com.pyin.plugin.client.api.http.PyinCenterHttpClient;
import com.pyin.plugin.client.core.config.PyinClientProperties;
import org.springframework.context.ApplicationEventPublisher;

public class DefaultPyinClientFeatureContext implements PyinClientFeatureContext {

    private final PyinCenterHttpClient httpClient;
    private final PyinClientProperties properties;
    private final ApplicationEventPublisher eventPublisher;

    public DefaultPyinClientFeatureContext(
            PyinCenterHttpClient httpClient,
            PyinClientProperties properties,
            ApplicationEventPublisher eventPublisher
    ) {
        this.httpClient = httpClient;
        this.properties = properties;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public PyinCenterHttpClient httpClient() {
        return httpClient;
    }

    @Override
    public Object properties() {
        return properties;
    }

    @Override
    public void publishLocalEvent(Object event) {
        eventPublisher.publishEvent(event);
    }
}
