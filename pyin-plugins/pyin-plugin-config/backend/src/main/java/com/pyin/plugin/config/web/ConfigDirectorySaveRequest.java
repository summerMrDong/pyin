package com.pyin.plugin.config.web;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ConfigDirectorySaveRequest {

    private Long id;
    private Long namespaceId;
    private Long parentId;
    private String name;
    private String description;
    private Integer sortOrder;
}
