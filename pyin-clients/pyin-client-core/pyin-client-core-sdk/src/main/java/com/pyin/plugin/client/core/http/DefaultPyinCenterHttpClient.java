package com.pyin.plugin.client.core.http;

import com.pyin.plugin.client.api.http.PyinCenterHttpClient;
import com.pyin.plugin.client.core.auth.TokenManager;
import com.pyin.plugin.client.core.config.PyinClientProperties;
import org.springframework.web.client.RestClient;

public class DefaultPyinCenterHttpClient implements PyinCenterHttpClient {

    private final RestClient restClient;
    private final TokenManager tokenManager;

    public DefaultPyinCenterHttpClient(PyinClientProperties properties, TokenManager tokenManager) {
        this.restClient = RestClient.builder().baseUrl(properties.getServerUrl()).build();
        this.tokenManager = tokenManager;
    }

    @Override
    public String get(String path) {
        return restClient.get()
                .uri(path)
                .header("Authorization", tokenManager.currentToken())
                .retrieve()
                .body(String.class);
    }

    @Override
    public String post(String path, Object body) {
        return restClient.post()
                .uri(path)
                .header("Authorization", tokenManager.currentToken())
                .body(body)
                .retrieve()
                .body(String.class);
    }
}
