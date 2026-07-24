package com.pyin.plugin.config.web;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ConfigNamespaceSaveRequest {

    private Long id;
    private String namespaceCode;
    private String env;
    private String displayName;
    private String description;
    private String directoryMode;

}
