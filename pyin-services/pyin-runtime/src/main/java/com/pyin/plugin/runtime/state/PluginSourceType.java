package com.pyin.plugin.runtime.state;

/**
 * 插件来源类型。
 */
public enum PluginSourceType {
    /** 随中心一同启动的内嵌系统插件。 */
    EMBEDDED_SYSTEM,
    /** 通过注册协议接入的独立插件节点。 */
    STANDALONE_NODE
}
