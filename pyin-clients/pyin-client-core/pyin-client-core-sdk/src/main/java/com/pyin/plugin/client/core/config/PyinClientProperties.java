package com.pyin.plugin.client.core.config;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PyinClientProperties {

    private boolean enabled = true;
    private String serverUrl;
    private Auth auth = new Auth();
    private Notify notify = new Notify();
    private Config config = new Config();

    @Getter
    @Setter
    public static class Auth {
        private String mode = "ACCESS_KEY";
        private String accessKey;
        private String accessSecret;

    }

    @Getter
    @Setter
    public static class Notify {
        private boolean enabled = true;
        private String mode = "SSE";

    }

    @Getter
    @Setter
    public static class Config {
        private String namespace;
        private String env;
        private boolean cacheEnabled = true;
        private boolean watchEnabled = true;
    }
}
