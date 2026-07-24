package com.pyin.plugin.system.plugin.entity;

import lombok.Getter;
import lombok.Setter;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

@TableName("pyin_plugin_api")
@Getter
@Setter
public class PluginApiEntity {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String pluginId;

    private String path;

    private String method;

    private String accessMode;

    private String permissionCode;

    private Boolean auditEnabled;

}
