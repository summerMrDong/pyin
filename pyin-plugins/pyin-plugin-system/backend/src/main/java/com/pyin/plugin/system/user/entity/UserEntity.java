package com.pyin.plugin.system.user.entity;

import lombok.Getter;
import lombok.Setter;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@TableName("pyin_user")
@Getter
@Setter
public class UserEntity {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String username;

    private String displayName;

    private String passwordHash;

    private String status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

}
