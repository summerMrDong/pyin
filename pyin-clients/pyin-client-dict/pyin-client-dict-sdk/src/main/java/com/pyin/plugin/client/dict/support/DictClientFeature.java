package com.pyin.plugin.client.dict.support;

import com.pyin.plugin.client.api.PyinClientFeature;
import com.pyin.plugin.client.api.PyinClientFeatureContext;
import com.pyin.plugin.client.api.event.PyinCenterEvent;
import com.pyin.plugin.client.dict.PyinDictClient;
import com.pyin.plugin.client.dict.model.DictItem;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class DictClientFeature implements PyinClientFeature, PyinDictClient {

    private final Map<String, List<DictItem>> cache = new ConcurrentHashMap<>();

    @Override
    public String featureCode() {
        return "dict";
    }

    @Override
    public String featureName() {
        return "Dict Client";
    }

    @Override
    public void initialize(PyinClientFeatureContext context) {
    }

    @Override
    public List<String> supportedEventTypes() {
        return List.of("dict.changed", "dict.refreshed");
    }

    @Override
    public void handleEvent(PyinCenterEvent event) {
        String typeCode = String.valueOf(event.getPayload().getOrDefault("typeCode", "default"));
        cache.putIfAbsent(typeCode, new ArrayList<>());
    }

    @Override
    public String getLabel(String typeCode, String itemValue) {
        return cache.getOrDefault(typeCode, List.of()).stream()
                .filter(item -> item.itemValue().equals(itemValue))
                .map(DictItem::itemLabel)
                .findFirst()
                .orElse(null);
    }

    @Override
    public List<DictItem> getItems(String typeCode) {
        return List.copyOf(cache.getOrDefault(typeCode, List.of()));
    }

    @Override
    public Map<String, String> getDictMap(String typeCode) {
        return cache.getOrDefault(typeCode, List.of()).stream()
                .collect(java.util.stream.Collectors.toMap(DictItem::itemValue, DictItem::itemLabel, (left, right) -> left));
    }
}
