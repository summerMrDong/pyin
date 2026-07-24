package com.pyin.plugin.system.role.entity;

import lombok.Getter;
import lombok.Setter;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@TableName("pyin_role")
@Getter
@Setter
public class RoleEntity {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String code;

    private String name;

    private String description;

    private Integer sort;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

}
