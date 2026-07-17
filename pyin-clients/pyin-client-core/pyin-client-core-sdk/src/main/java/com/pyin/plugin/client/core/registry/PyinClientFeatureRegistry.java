package com.pyin.plugin.client.core.registry;

import com.pyin.plugin.client.api.PyinClientFeature;
import com.pyin.plugin.client.api.event.PyinCenterEvent;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class PyinClientFeatureRegistry {

    private final Map<String, PyinClientFeature> featureMap = new ConcurrentHashMap<>();

    public void register(PyinClientFeature feature) {
        featureMap.put(feature.featureCode(), feature);
    }

    public void dispatch(PyinCenterEvent event) {
        PyinClientFeature feature = featureMap.get(event.getFeatureCode());
        if (feature == null) {
            return;
        }
        if (!feature.supportedEventTypes().contains(event.getEventType())) {
            return;
        }
        feature.handleEvent(event);
    }
}
