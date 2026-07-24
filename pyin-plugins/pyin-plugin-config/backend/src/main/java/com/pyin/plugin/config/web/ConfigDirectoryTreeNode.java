package com.pyin.plugin.config.web;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ConfigDirectoryTreeNode {

    private Long id;
    private Long parentId;
    private String name;
    private String description;
    private Integer sortOrder;
    private Integer itemCount;
    private List<ConfigDirectoryTreeNode> children = new ArrayList<>();
}
