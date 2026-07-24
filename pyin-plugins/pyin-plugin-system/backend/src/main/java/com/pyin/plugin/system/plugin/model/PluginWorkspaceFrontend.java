package com.pyin.plugin.system.plugin.model;

/**
 * 插件工作区的模块联邦加载信息。
 *
 * <p>仅供主前端壳发现和注册远程模块使用。联邦远端名称固定等于插件 ID，壳应用固定加载
 * {@code ./routes} 路由入口；其他模块由消费方按约定的模块名显式加载，无须中心登记。</p>
 *
 * @param remoteEntry 模块联邦入口地址，用于加载该插件的 {@code remoteEntry.js}
 */
public record PluginWorkspaceFrontend(
        String remoteEntry
) {
}
