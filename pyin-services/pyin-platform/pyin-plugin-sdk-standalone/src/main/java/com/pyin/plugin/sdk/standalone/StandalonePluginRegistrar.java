package com.pyin.plugin.sdk.standalone;

import com.pyin.plugin.spi.model.PluginNodeRegistration;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

public class StandalonePluginRegistrar {

    private final RestClient restClient;

    public StandalonePluginRegistrar(RestClient.Builder builder) {
        this.restClient = builder.build();
    }

    public void register(String centerUrl, PluginNodeRegistration registration) {
        restClient.post()
                .uri(centerUrl + "/api/core/plugin-nodes/register")
                .contentType(MediaType.APPLICATION_JSON)
                .body(registration)
                .retrieve()
                .toBodilessEntity();
    }

    public void heartbeat(String centerUrl, String pluginId, String nodeId) {
        restClient.post()
                .uri(centerUrl + "/api/core/plugin-nodes/" + pluginId + "/" + nodeId + "/heartbeat")
                .retrieve()
                .toBodilessEntity();
    }

    public void offline(String centerUrl, String pluginId, String nodeId) {
        restClient.post()
                .uri(centerUrl + "/api/core/plugin-nodes/" + pluginId + "/" + nodeId + "/offline")
                .retrieve()
                .toBodilessEntity();
    }
}
