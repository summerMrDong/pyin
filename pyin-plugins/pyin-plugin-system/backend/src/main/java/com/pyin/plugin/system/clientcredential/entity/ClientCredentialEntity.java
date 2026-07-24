package com.pyin.plugin.system.clientcredential.entity;

import lombok.Getter;
import lombok.Setter;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@TableName("pyin_client_credential")
@Getter
@Setter
public class ClientCredentialEntity {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String credentialName;

    private String accessKey;

    private String accessSecretHash;

    private String accessSecretEncrypted;

    private String status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

}
