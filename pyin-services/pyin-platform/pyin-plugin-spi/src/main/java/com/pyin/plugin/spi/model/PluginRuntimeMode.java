package com.pyin.plugin.spi.model;

/**
 * 插件运行模式。
 */
public enum PluginRuntimeMode {
    /** 与中心运行在同一个 JVM 中。 */
    EMBEDDED,
    /** 以独立进程方式运行。 */
    STANDALONE
}
