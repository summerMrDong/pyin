package com.pyin.plugin.system.resource.entity;

import lombok.Getter;
import lombok.Setter;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@TableName("pyin_role_resource")
@Getter
@Setter
public class RoleResourceEntity {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long roleId;
    private String resourceCode;
    private String resourceScope;
    private LocalDateTime createdAt;

}
