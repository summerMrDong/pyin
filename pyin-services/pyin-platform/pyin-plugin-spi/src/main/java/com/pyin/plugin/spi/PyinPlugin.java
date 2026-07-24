package com.pyin.plugin.spi;

import com.pyin.plugin.spi.model.PluginManifest;

/**
 * Pyin 插件的后端入口契约。
 *
 * <p>插件运行时通过该接口识别插件并读取其基础元数据。该接口可由插件 backend 模块实现，
 * 不用于跨插件传递业务能力；跨模块公共能力应声明在插件独立的 {@code api} 模块中。</p>
 */
public interface PyinPlugin {

    /**
     * 返回插件基础清单。
     *
     * <p>运行时会基于该清单装配远程前端入口，并结合注解扫描生成接口、权限和资源信息。
     * 返回值不得为 {@code null}，且必须通过 {@link PluginManifest#builder(String)} 提供插件 ID，
     * 不得包含页面菜单或手工接口清单。</p>
     *
     * @return 非空插件清单
     * @throws IllegalArgumentException 当清单缺少必填字段或字段不符合运行时约束时由装配阶段抛出
     */
    PluginManifest manifest();

}
