package com.pyin.plugin.system.plugin.entity;

import lombok.Getter;
import lombok.Setter;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

@TableName("pyin_plugin_resource")
@Getter
@Setter
public class PluginResourceEntity {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String pluginId;
    private String resourceCode;
    private String resourceName;
    private String resourceType;
    private String parentCode;
    private String path;
    private String icon;
    private Integer sort;
    private String permissionCode;
    private Boolean visible;
    private String metadataJson;

}
