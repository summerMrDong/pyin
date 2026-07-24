package com.pyin.plugin.system.api.service;

/**
 * 系统插件对外暴露的 C 端认证请求审计公共能力契约。
 *
 * <p>该接口供 auth 模块记录 C 端 SDK 获取 token 等认证相关请求。审计数据归属
 * system 插件的 clientcredential 领域，调用方只提交审计事件，不直接写入请求日志表。</p>
 */
public interface SystemClientRequestAuditPublicService {

    /**
     * 表示认证请求处理成功的审计状态。
     */
    String STATUS_SUCCESS = "SUCCESS";

    /**
     * 表示认证请求处理失败的审计状态。
     */
    String STATUS_FAILED = "FAILED";

    /**
     * 表示 C 端 SDK 首次换取 token 的请求类型。
     */
    String TYPE_AUTH_TOKEN = "AUTH_TOKEN";

    /**
     * 写入一条 C 端认证请求审计日志。
     *
     * <p>成功请求应提供凭证 ID、Access Key、请求类型、URI、HTTP 方法、客户端 IP 和成功状态。
     * 失败请求应尽可能提供可识别的凭证信息，并填写失败编码与失败原因。该方法只负责记录
     * 审计事实，不参与认证决策。</p>
     *
     * @param credentialId 接入凭证主键 ID；凭证无法识别时可为 {@code null}。
     * @param accessKey C 端接入凭证公开 Key；请求未携带或无法识别时可为 {@code null}。
     * @param requestType 请求类型，例如 {@link #TYPE_AUTH_TOKEN}。
     * @param requestUri 请求 URI。
     * @param httpMethod HTTP 方法，例如 {@code GET}、{@code POST}。
     * @param clientIp 调用方客户端 IP。
     * @param requestStatus 请求处理状态，例如 {@link #STATUS_SUCCESS} 或 {@link #STATUS_FAILED}。
     * @param failureCode 失败编码；成功请求可为 {@code null}。
     * @param failureMessage 失败原因描述；成功请求可为 {@code null}。
     */
    void log(
            Long credentialId,
            String accessKey,
            String requestType,
            String requestUri,
            String httpMethod,
            String clientIp,
            String requestStatus,
            String failureCode,
            String failureMessage
    );
}
