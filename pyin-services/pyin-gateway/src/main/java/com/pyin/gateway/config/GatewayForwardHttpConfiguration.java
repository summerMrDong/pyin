package com.pyin.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.RestTemplate;

@Configuration
public class GatewayForwardHttpConfiguration {

    @Bean
    RestTemplate gatewayForwardRestTemplate() {
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.setErrorHandler(new DefaultResponseErrorHandler() {
            @Override
            protected boolean hasError(HttpStatusCode statusCode) {
                return false;
            }
        });
        return restTemplate;
    }
}
