package com.pyin.plugin.system.permission.entity;

import lombok.Getter;
import lombok.Setter;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

@TableName("pyin_plugin_permission")
@Getter
@Setter
public class PluginPermissionEntity {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String pluginId;

    private String permissionCode;

    private String permissionName;

    private String resourceType;

}
