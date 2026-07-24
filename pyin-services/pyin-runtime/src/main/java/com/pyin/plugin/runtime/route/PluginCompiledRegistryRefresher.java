package com.pyin.plugin.runtime.route;

import com.pyin.plugin.spi.model.ResolvedPluginDescriptor;
import org.springframework.stereotype.Component;

/**
 * 插件 API 编译注册表刷新器。
 *
 * <p>该组件提供给插件元数据同步流程调用。同步流程可以来自内嵌插件启动、独立插件注册或后续
 * 插件升级，但最终都会在运行时层刷新同一份 API 匹配事实。</p>
 */
@Component
public class PluginCompiledRegistryRefresher {

    private final CompiledPluginApiRegistry compiledPluginApiRegistry;

    public PluginCompiledRegistryRefresher(CompiledPluginApiRegistry compiledPluginApiRegistry) {
        this.compiledPluginApiRegistry = compiledPluginApiRegistry;
    }

    /**
     * 刷新指定插件的 API 规则。
     *
     * @param descriptor 插件运行时描述对象；为空或插件 ID 为空时不做处理。
     */
    public void refresh(ResolvedPluginDescriptor descriptor) {
        if (descriptor == null || descriptor.getPluginId() == null || descriptor.getPluginId().isBlank()) {
            return;
        }
        CompiledPluginApiRegistry.CompiledRegistrySnapshot compiledSnapshot =
                compiledPluginApiRegistry.compile(descriptor);
        compiledPluginApiRegistry.replace(compiledSnapshot);
    }
}
