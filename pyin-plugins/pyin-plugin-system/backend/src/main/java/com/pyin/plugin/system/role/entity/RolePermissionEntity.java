package com.pyin.plugin.system.role.entity;

import lombok.Getter;
import lombok.Setter;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

@TableName("pyin_role_permission")
@Getter
@Setter
public class RolePermissionEntity {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long roleId;

    private String permissionCode;

}
