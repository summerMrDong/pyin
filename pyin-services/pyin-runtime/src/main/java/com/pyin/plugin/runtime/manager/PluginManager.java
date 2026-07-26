package com.pyin.plugin.runtime.manager;

import com.pyin.plugin.runtime.loader.PluginRuntimeProperties;
import com.pyin.plugin.runtime.registry.PluginRegistry;
import com.pyin.plugin.runtime.registry.RegisteredPlugin;
import com.pyin.plugin.sdk.manifest.PluginDescriptorAssembler;
import com.pyin.plugin.spi.PluginMetadataSynchronizer;
import com.pyin.plugin.spi.PyinPlugin;
import com.pyin.plugin.spi.model.ResolvedPluginDescriptor;
import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ansi.AnsiColor;
import org.springframework.boot.ansi.AnsiOutput;
import org.springframework.boot.ansi.AnsiStyle;
import org.springframework.context.ApplicationContext;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;

@Service
@EnableConfigurationProperties(PluginRuntimeProperties.class)
public class PluginManager {

    private static final Logger log = LoggerFactory.getLogger(PluginManager.class);
    private static final int TABLE_INNER_WIDTH = 86;
    private static final String HORIZONTAL_BORDER = "─".repeat(TABLE_INNER_WIDTH);

    private final PluginRuntimeProperties properties;
    private final PluginRegistry pluginRegistry;
    private final List<PyinPlugin> embeddedPlugins;
    private final PluginDescriptorAssembler pluginDescriptorAssembler;
    private final ApplicationContext applicationContext;
    private final ObjectProvider<PluginMetadataSynchronizer> pluginMetadataSynchronizerProvider;

    public PluginManager(
            PluginRuntimeProperties properties,
            PluginRegistry pluginRegistry,
            List<PyinPlugin> embeddedPlugins,
            PluginDescriptorAssembler pluginDescriptorAssembler,
            ApplicationContext applicationContext,
            ObjectProvider<PluginMetadataSynchronizer> pluginMetadataSynchronizerProvider
    ) {
        this.properties = properties;
        this.pluginRegistry = pluginRegistry;
        this.embeddedPlugins = embeddedPlugins;
        this.pluginDescriptorAssembler = pluginDescriptorAssembler;
        this.applicationContext = applicationContext;
        this.pluginMetadataSynchronizerProvider = pluginMetadataSynchronizerProvider;
    }

    @PostConstruct
    public void bootstrap() throws IOException {
        registerEmbeddedPlugins();
        logLoadedPlugins();
    }

    private void registerEmbeddedPlugins() {
        Path sourcePluginsDir = Path.of(properties.getSourcePluginsDir());
        embeddedPlugins.stream()
                .sorted(Comparator.comparing(plugin -> plugin.manifest().getPluginId()))
                .forEach(plugin -> registerEmbeddedPlugin(plugin, sourcePluginsDir));
    }

    private void registerEmbeddedPlugin(PyinPlugin plugin, Path sourcePluginsDir) {
        try {
            ResolvedPluginDescriptor descriptor = pluginDescriptorAssembler.assemble(applicationContext, plugin);
            if (descriptor == null) {
                log.warn("Skip embedded plugin '{}' because manifest() returned null", plugin.getClass().getName());
                return;
            }
            if (!properties.getEmbeddedPluginIds().contains(descriptor.getPluginId())) {
                log.warn("Skip plugin '{}' because it is not approved for the embedded runtime", descriptor.getPluginId());
                return;
            }
            pluginMetadataSynchronizerProvider.ifAvailable(synchronizer -> synchronizer.sync(descriptor));
            pluginRegistry.registerEmbedded(descriptor, plugin,
                    resolveEmbeddedPluginHome(sourcePluginsDir, descriptor.getPluginId()));
        } catch (Exception exception) {
            log.warn("Failed to register embedded plugin '{}': {}", plugin.getClass().getName(), exception.getMessage());
        }
    }

    private java.nio.file.Path resolveEmbeddedPluginHome(java.nio.file.Path sourcePluginsDir, String pluginId) {
        java.nio.file.Path artifactPath = sourcePluginsDir.resolve("pyin-plugin-" + pluginId);
        if (java.nio.file.Files.exists(artifactPath)) {
            return artifactPath;
        }
        java.nio.file.Path plainIdPath = sourcePluginsDir.resolve(pluginId);
        if (java.nio.file.Files.exists(plainIdPath)) {
            return plainIdPath;
        }
        return java.nio.file.Path.of(properties.getSystemPluginsDir()).resolve(pluginId);
    }

    private void logLoadedPlugins() {
        List<RegisteredPlugin> plugins = pluginRegistry.all().stream()
                .sorted(Comparator.comparing(RegisteredPlugin::pluginId))
                .toList();
        log.info("{}", buildLoadedPluginsSummary(plugins));
    }

    static String buildLoadedPluginsSummary(List<RegisteredPlugin> plugins) {
        StringBuilder summary = new StringBuilder();
        summary.append(System.lineSeparator())
                .append(color("┌──────────────────── Pyin 已加载插件 (" + plugins.size() + ")",
                        AnsiStyle.BOLD, AnsiColor.CYAN))
                .append(System.lineSeparator())
                .append(color(row(
                        " " + fit("ID", 21)
                                + " " + fit("名称", 16)
                                + " " + fit("版本", 10)
                                + " " + fit("来源", 17)
                                + " " + fit("状态", 10)
                ), AnsiStyle.BOLD, AnsiColor.YELLOW))
                .append(System.lineSeparator())
                .append(color("├" + HORIZONTAL_BORDER, AnsiColor.CYAN));
        for (RegisteredPlugin plugin : plugins) {
            summary.append(System.lineSeparator()).append(color(row(
                    " " + fit(plugin.pluginId(), 21)
                            + " " + fit(plugin.descriptor().getPluginName(), 16)
                            + " " + fit(plugin.descriptor().getPluginVersion(), 10)
                            + " " + fit(String.valueOf(plugin.sourceType()), 17)
                            + " " + fit(String.valueOf(plugin.status()), 10)
            ), AnsiColor.GREEN));
            summary.append(System.lineSeparator()).append(color(
                    row("   后端入口：" + value(plugin.descriptor().getBasePath())),
                    AnsiColor.BLUE
            ));
            summary.append(System.lineSeparator()).append(color(
                    row("   前端入口：" + value(plugin.descriptor().getEntryJs())),
                    AnsiColor.BLUE
            ));
        }
        summary.append(System.lineSeparator())
                .append(color("└" + HORIZONTAL_BORDER, AnsiColor.CYAN));
        return summary.toString();
    }

    private static String row(String content) {
        return "│" + content.stripTrailing();
    }

    private static String fit(String value, int width) {
        String text = value(value);
        int textWidth = displayWidth(text);
        if (textWidth <= width) {
            return text + " ".repeat(width - textWidth);
        }

        int targetWidth = Math.max(0, width - 3);
        StringBuilder truncated = new StringBuilder();
        int currentWidth = 0;
        for (int offset = 0; offset < text.length();) {
            int codePoint = text.codePointAt(offset);
            int codePointWidth = displayWidth(codePoint);
            if (currentWidth + codePointWidth > targetWidth) {
                break;
            }
            truncated.appendCodePoint(codePoint);
            currentWidth += codePointWidth;
            offset += Character.charCount(codePoint);
        }
        return truncated + "..." + " ".repeat(Math.max(0, width - currentWidth - 3));
    }

    static int displayWidth(String value) {
        return value.codePoints().map(PluginManager::displayWidth).sum();
    }

    private static int displayWidth(int codePoint) {
        Character.UnicodeBlock block = Character.UnicodeBlock.of(codePoint);
        return block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS
                || block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A
                || block == Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS
                || block == Character.UnicodeBlock.CJK_SYMBOLS_AND_PUNCTUATION
                || block == Character.UnicodeBlock.HALFWIDTH_AND_FULLWIDTH_FORMS
                || block == Character.UnicodeBlock.HANGUL_SYLLABLES
                || block == Character.UnicodeBlock.HIRAGANA
                || block == Character.UnicodeBlock.KATAKANA ? 2 : 1;
    }

    private static String value(Object value) {
        return value == null ? "-" : value.toString();
    }

    private static String color(String value, Object... colors) {
        Object[] elements = new Object[colors.length + 3];
        System.arraycopy(colors, 0, elements, 0, colors.length);
        elements[colors.length] = value;
        elements[colors.length + 1] = AnsiStyle.NORMAL;
        elements[colors.length + 2] = AnsiColor.DEFAULT;
        return AnsiOutput.toString(elements);
    }
}
