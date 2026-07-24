package com.pyin.plugin.system.audit.entity;

import lombok.Getter;
import lombok.Setter;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

@TableName("pyin_audit_log")
@Getter
@Setter
public class AuditLogEntity {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String action;

    private String operatorName;

}
