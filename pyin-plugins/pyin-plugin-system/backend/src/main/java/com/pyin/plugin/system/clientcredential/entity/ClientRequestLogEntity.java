package com.pyin.plugin.system.clientcredential.entity;

import lombok.Getter;
import lombok.Setter;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@TableName("pyin_client_request_log")
@Getter
@Setter
public class ClientRequestLogEntity {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long credentialId;

    private String accessKey;

    private String requestType;

    private String requestUri;

    private String httpMethod;

    private String clientIp;

    private String requestStatus;

    private String failureCode;

    private String failureMessage;

    private LocalDateTime createdAt;

}
