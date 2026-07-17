# Runtime Plugin Modules

`pyin-plugins/` 现在只承载插件源码模块，不再表示部署运行目录。

当前插件模块：

- `pyin-plugins/pyin-plugin-config`
- `pyin-plugins/pyin-plugin-dict`
- `pyin-plugins/pyin-plugin-file`
- `pyin-plugins/pyin-plugin-city-snapshot-admin`
- `pyin-plugins/pyin-plugin-city-snapshot-department`
- `pyin-plugins/pyin-plugin-city-snapshot-citizen`

每个插件模块内部包含：

- `backend/`
- `frontend/`
- Java `PluginManifest` 声明入口

说明：

- 插件元数据唯一事实来源是 Java `PluginManifest` 与 `PyinPlugin.manifest()`
- `plugin.yml` 已从当前实现中退役，不应再作为新插件开发产物
