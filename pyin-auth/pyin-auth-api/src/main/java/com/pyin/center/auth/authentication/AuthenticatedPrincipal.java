package com.pyin.center.auth.authentication;

/**
 * 网关已认证主体。
 *
 * <p>该模型由 auth 领域返回给 gateway 领域，用于表达“当前请求是谁发起的”。它只包含可安全
 * 传递给审计、转发签名和下游插件读取的主体标识，不包含密码、Secret、Token 或权限集合等敏感
 * 认证材料。</p>
 *
 * @param principalType 主体类型，例如 {@code ADMIN_USER} 或 {@code CLIENT_CREDENTIAL}。
 * @param principalId 主体唯一标识；后台用户通常为用户 ID，C 端通常为凭证 ID。
 * @param displayName 主体展示名称或公开 Key，可为空。
 */
public record AuthenticatedPrincipal(
        String principalType,
        String principalId,
        String displayName
) {

    /**
     * 创建后台用户主体。
     *
     * @param userId 用户主键 ID。
     * @param displayName 用户展示名称或账号。
     * @return 后台用户网关主体。
     */
    public static AuthenticatedPrincipal adminUser(Long userId, String displayName) {
        return new AuthenticatedPrincipal("ADMIN_USER", userId == null ? "" : String.valueOf(userId), displayName);
    }

    /**
     * 创建 C 端接入凭证主体。
     *
     * @param credentialId 凭证主键 ID。
     * @param accessKey 凭证公开 Key，不包含 Secret。
     * @return C 端接入凭证网关主体。
     */
    public static AuthenticatedPrincipal clientCredential(Long credentialId, String accessKey) {
        return new AuthenticatedPrincipal("CLIENT_CREDENTIAL", credentialId == null ? "" : String.valueOf(credentialId), accessKey);
    }
}
