package com.pyin.plugin.client.api;

import com.pyin.plugin.client.api.event.PyinCenterEvent;
import java.util.List;

public interface PyinClientFeature {

    String featureCode();

    String featureName();

    void initialize(PyinClientFeatureContext context);

    List<String> supportedEventTypes();

    void handleEvent(PyinCenterEvent event);
}
