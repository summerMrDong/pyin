package com.pyin.plugin.config.web;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ConfigDirectoryMoveRequest {

    private Long parentId;
    private Integer sortOrder;
}
