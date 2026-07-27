package com.pyin.plugin.config.web;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ConfigItemSaveRequest {

    private Long id;
    private Long namespaceId;
    private Long directoryId;
    private String itemKey;
    private String itemValue;
    private String defaultValue;
    private String valueType;
    private String status;
    private String description;

}
