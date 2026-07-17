package com.pyin.plugin.client.core.config;

public class PyinClientProperties {

    private boolean enabled = true;
    private String serverUrl;
    private Auth auth = new Auth();
    private Notify notify = new Notify();
    private Config config = new Config();

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

    public void setAuth(Auth auth) {
        this.auth = auth;
    }

    public Notify getNotify() {
        return notify;
    }

    public void setNotify(Notify notify) {
        this.notify = notify;
    }

    public Config getConfig() {
        return config;
    }

    public void setConfig(Config config) {
        this.config = config;
    }

    public static class Auth {
        private String mode = "ACCESS_KEY";
        private String accessKey;
        private String accessSecret;
        private boolean autoRefresh = true;
        private int tokenRefreshBeforeSeconds = 300;

        public String getMode() {
            return mode;
        }

        public void setMode(String mode) {
            this.mode = mode;
        }

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

        public boolean isAutoRefresh() {
            return autoRefresh;
        }

        public void setAutoRefresh(boolean autoRefresh) {
            this.autoRefresh = autoRefresh;
        }

        public int getTokenRefreshBeforeSeconds() {
            return tokenRefreshBeforeSeconds;
        }

        public void setTokenRefreshBeforeSeconds(int tokenRefreshBeforeSeconds) {
            this.tokenRefreshBeforeSeconds = tokenRefreshBeforeSeconds;
        }
    }

    public static class Notify {
        private boolean enabled = true;
        private String mode = "SSE";

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getMode() {
            return mode;
        }

        public void setMode(String mode) {
            this.mode = mode;
        }
    }

    public static class Config {
        private String namespace;
        private String env;
        private boolean cacheEnabled = true;
        private boolean watchEnabled = true;

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

        public boolean isCacheEnabled() {
            return cacheEnabled;
        }

        public void setCacheEnabled(boolean cacheEnabled) {
            this.cacheEnabled = cacheEnabled;
        }

        public boolean isWatchEnabled() {
            return watchEnabled;
        }

        public void setWatchEnabled(boolean watchEnabled) {
            this.watchEnabled = watchEnabled;
        }
    }
}
