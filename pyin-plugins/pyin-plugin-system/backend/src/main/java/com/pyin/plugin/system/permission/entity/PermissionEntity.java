package com.pyin.plugin.system.permission.entity;

import lombok.Getter;
import lombok.Setter;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

@TableName("pyin_permission")
@Getter
@Setter
public class PermissionEntity {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String code;

    private String name;

}
