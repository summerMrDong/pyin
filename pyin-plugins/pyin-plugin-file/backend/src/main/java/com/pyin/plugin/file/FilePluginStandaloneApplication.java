package com.pyin.plugin.file;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication(scanBasePackages = "com.pyin")
@ConfigurationPropertiesScan(basePackages = "com.pyin")
public class FilePluginStandaloneApplication {

    public static void main(String[] args) {
        SpringApplication.run(FilePluginStandaloneApplication.class, args);
    }
}
