package com.pyin.plugin.system.plugin.entity;

import lombok.Getter;
import lombok.Setter;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

@TableName("pyin_plugin")
@Getter
@Setter
public class PluginEntity {

    @TableId
    private String pluginId;

    private String pluginName;

    private String version;

}
