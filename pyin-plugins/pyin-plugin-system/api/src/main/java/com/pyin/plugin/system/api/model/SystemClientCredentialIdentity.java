package com.pyin.plugin.system.api.model;

/**
 * C 端接入凭证身份视图。
 *
 * <p>该模型用于 auth 模块进行 C 端 SDK 认证和已签发 token 校验。模型只包含公开 Key、Secret
 * 哈希和启停状态，不包含明文 Secret；明文 Secret 必须通过公共服务按需解密获取。</p>
 *
 * @param id 接入凭证主键 ID。
 * @param accessKey 接入凭证公开 Key，可用于识别调用方。
 * @param accessSecretHash 接入凭证 Secret 哈希，供认证模块二次校验明文 Secret 使用。
 * @param enabled 凭证是否启用；为 {@code false} 时调用方应拒绝 token 签发或后续请求鉴权。
 */
public record SystemClientCredentialIdentity(
        Long id,
        String accessKey,
        String accessSecretHash,
        boolean enabled
) {
}
