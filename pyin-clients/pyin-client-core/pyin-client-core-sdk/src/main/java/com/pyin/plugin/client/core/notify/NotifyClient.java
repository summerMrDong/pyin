package com.pyin.plugin.client.core.notify;

import com.pyin.plugin.client.api.event.PyinCenterEvent;
import com.pyin.plugin.client.core.registry.PyinClientFeatureRegistry;
import java.util.function.Consumer;

public class NotifyClient {

    private final PyinClientFeatureRegistry featureRegistry;

    public NotifyClient(PyinClientFeatureRegistry featureRegistry) {
        this.featureRegistry = featureRegistry;
    }

    public void accept(PyinCenterEvent event) {
        featureRegistry.dispatch(event);
    }

    public Consumer<PyinCenterEvent> consumer() {
        return this::accept;
    }
}
