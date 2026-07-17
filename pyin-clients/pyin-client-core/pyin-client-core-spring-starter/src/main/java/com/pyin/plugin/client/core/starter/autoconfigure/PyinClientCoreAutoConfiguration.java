package com.pyin.plugin.client.core.starter.autoconfigure;

import com.pyin.plugin.client.api.http.PyinCenterHttpClient;
import com.pyin.plugin.client.core.auth.TokenManager;
import com.pyin.plugin.client.core.config.PyinClientProperties;
import com.pyin.plugin.client.core.context.DefaultPyinClientFeatureContext;
import com.pyin.plugin.client.core.http.DefaultPyinCenterHttpClient;
import com.pyin.plugin.client.core.notify.NotifyClient;
import com.pyin.plugin.client.core.registry.PyinClientFeatureRegistry;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
@EnableConfigurationProperties(PyinClientCoreAutoConfiguration.PyinPropertiesBinding.class)
public class PyinClientCoreAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public PyinClientProperties pyinClientProperties(PyinPropertiesBinding binding) {
        PyinClientProperties properties = new PyinClientProperties();
        properties.setEnabled(binding.isEnabled());
        properties.setServerUrl(binding.getServerUrl());
        properties.getAuth().setAccessKey(binding.getAuth().getAccessKey());
        properties.getAuth().setAccessSecret(binding.getAuth().getAccessSecret());
        properties.getConfig().setNamespace(binding.getConfig().getNamespace());
        properties.getConfig().setEnv(binding.getConfig().getEnv());
        return properties;
    }

    @Bean
    @ConditionalOnMissingBean
    public TokenManager tokenManager() {
        return new TokenManager();
    }

    @Bean
    @ConditionalOnMissingBean
    public PyinCenterHttpClient pyinCenterHttpClient(PyinClientProperties properties, TokenManager tokenManager) {
        return new DefaultPyinCenterHttpClient(properties, tokenManager);
    }

    @Bean
    @ConditionalOnMissingBean
    public PyinClientFeatureRegistry pyinClientFeatureRegistry() {
        return new PyinClientFeatureRegistry();
    }

    @Bean
    @ConditionalOnMissingBean
    public DefaultPyinClientFeatureContext pyinClientFeatureContext(
            PyinCenterHttpClient httpClient,
            PyinClientProperties properties,
            ApplicationEventPublisher publisher
    ) {
        return new DefaultPyinClientFeatureContext(httpClient, properties, publisher);
    }

    @Bean
    @ConditionalOnMissingBean
    public NotifyClient notifyClient(PyinClientFeatureRegistry registry) {
        return new NotifyClient(registry);
    }

    @ConfigurationProperties(prefix = "pyin.center")
    public static class PyinPropertiesBinding {
        private boolean enabled = true;
        private String serverUrl;
        private final Auth auth = new Auth();
        private final Config config = new Config();

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getServerUrl() {
            return serverUrl;
        }

        public void setServerUrl(String serverUrl) {
            this.serverUrl = serverUrl;
        }

        public Auth getAuth() {
            return auth;
        }

        public Config getConfig() {
            return config;
        }

        public static class Auth {
            private String accessKey;
            private String accessSecret;

            public String getAccessKey() {
                return accessKey;
            }

            public void setAccessKey(String accessKey) {
                this.accessKey = accessKey;
            }

            public String getAccessSecret() {
                return accessSecret;
            }

            public void setAccessSecret(String accessSecret) {
                this.accessSecret = accessSecret;
            }
        }

        public static class Config {
            private String namespace;
            private String env;

            public String getNamespace() {
                return namespace;
            }

            public void setNamespace(String namespace) {
                this.namespace = namespace;
            }

            public String getEnv() {
                return env;
            }

            public void setEnv(String env) {
                this.env = env;
            }
        }
    }
}
