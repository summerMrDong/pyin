package com.pyin.plugin.config.web;

public class ConfigNamespaceSaveRequest {

    private Long id;
    private String namespaceCode;
    private String env;
    private String displayName;
    private String description;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNamespaceCode() {
        return namespaceCode;
    }

    public void setNamespaceCode(String namespaceCode) {
        this.namespaceCode = namespaceCode;
    }

    public String getEnv() {
        return env;
    }

    public void setEnv(String env) {
        this.env = env;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
