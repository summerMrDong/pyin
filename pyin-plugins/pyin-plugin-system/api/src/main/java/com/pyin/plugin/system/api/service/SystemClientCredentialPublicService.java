package com.pyin.plugin.system.api.service;

import com.pyin.plugin.system.api.model.SystemClientCredentialIdentity;

/**
 * 系统插件对外暴露的 C 端接入凭证公共能力契约。
 *
 * <p>该接口供 auth 模块完成 C 端 SDK token 签发、票据校验和签名校验。接入凭证归属
 * system 插件的 clientcredential 领域，其他模块只应通过该 API 获取凭证身份和解密后的
 * Secret，不应直接访问凭证表或 system backend 内部 Service。</p>
 */
public interface SystemClientCredentialPublicService {

    /**
     * 根据 Access Key 查询接入凭证身份。
     *
     * <p>该方法用于 C 端 SDK 首次换取 token 时定位凭证。Access Key 不存在时返回
     * {@code null}；凭证禁用时返回对象中的 {@code enabled} 为 {@code false}。</p>
     *
     * @param accessKey C 端接入凭证的公开 Key。
     * @return 接入凭证身份；不存在时返回 {@code null}。
     */
    SystemClientCredentialIdentity findByAccessKey(String accessKey);

    /**
     * 根据凭证 ID 查询接入凭证身份。
     *
     * <p>该方法主要用于已签发 token 后续请求中的凭证有效性校验。凭证不存在时返回
     * {@code null}。</p>
     *
     * @param id 接入凭证主键 ID。
     * @return 接入凭证身份；不存在时返回 {@code null}。
     */
    SystemClientCredentialIdentity findById(Long id);

    /**
     * 解密接入凭证 Secret。
     *
     * <p>该方法只在签名校验时使用。返回值是短生命周期的明文 Secret，调用方应只在内存中
     * 即时使用，不应记录日志、返回前端或持久化。参数为空或凭证不存在时返回 {@code null}。</p>
     *
     * @param credential 接入凭证身份对象。
     * @return 解密后的明文 Secret；无法解密或凭证不存在时返回 {@code null}。
     */
    String decryptSecret(SystemClientCredentialIdentity credential);
}
