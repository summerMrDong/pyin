package com.pyin.plugin.runtime.route;

import com.pyin.plugin.spi.model.PluginAccessMode;
import org.springframework.web.util.pattern.PathPattern;

/**
 * 已编译的插件 API 路由规则。
 *
 * <p>该模型由运行时内部持有，用于快速匹配插件动态 API。它只描述插件已发布 API 的静态事实，
 * 不包含当前请求身份、授权结果或转发状态。</p>
 *
 * @param pluginId 插件 ID。
 * @param httpMethod HTTP 方法，已统一为大写。
 * @param rawPathPattern 插件声明的原始网关相对路径模式。
 * @param canonicalPathPattern 归一后的路径冲突检测模式，路径变量名会被抹平。
 * @param compiledPattern Spring PathPattern 编译结果。
 * @param internalPath 内嵌插件在中心 JVM 内真实 Controller 路径。
 * @param permissionCode 后台管理端权限编码；允许为空，表示只要求登录态。
 * @param accessMode API 访问模式。
 * @param auditEnabled 是否启用审计。
 */
public record CompiledApiRule(
        String pluginId,
        String httpMethod,
        String rawPathPattern,
        String canonicalPathPattern,
        PathPattern compiledPattern,
        String internalPath,
        String permissionCode,
        PluginAccessMode accessMode,
        boolean auditEnabled
) {
}
