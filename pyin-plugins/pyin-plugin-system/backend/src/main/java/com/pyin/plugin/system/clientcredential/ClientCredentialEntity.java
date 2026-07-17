package com.pyin.plugin.system.clientcredential;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@TableName("pyin_client_credential")
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

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCredentialName() {
        return credentialName;
    }

    public void setCredentialName(String credentialName) {
        this.credentialName = credentialName;
    }

    public String getAccessKey() {
        return accessKey;
    }

    public void setAccessKey(String accessKey) {
        this.accessKey = accessKey;
    }

    public String getAccessSecretHash() {
        return accessSecretHash;
    }

    public void setAccessSecretHash(String accessSecretHash) {
        this.accessSecretHash = accessSecretHash;
    }

    public String getAccessSecretEncrypted() {
        return accessSecretEncrypted;
    }

    public void setAccessSecretEncrypted(String accessSecretEncrypted) {
        this.accessSecretEncrypted = accessSecretEncrypted;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
