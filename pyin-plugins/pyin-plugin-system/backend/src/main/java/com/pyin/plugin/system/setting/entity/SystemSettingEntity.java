package com.pyin.plugin.system.setting.entity;

import lombok.Getter;
import lombok.Setter;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

@TableName("pyin_system_setting")
@Getter
@Setter
public class SystemSettingEntity {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String settingKey;

    private String settingValue;

}
