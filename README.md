# Pyin 配置中心 P1

`pyin-config-center` 是一个插件化配置中心平台骨架工程。

## 基础选型

- 后端：`Spring Boot 3 + Java 17 + Sa-Token + MyBatis-Plus`
- 前端：`Vue 3 + Vite + 模块联邦`
- 默认本地数据库：`H2 (file mode, MySQL compatibility)`
- 生产接入能力：`MySQL 8`

## 仓库分区说明

- `pyin-services/`：服务端 Java 模块，包括启动入口、核心平台、插件运行时、插件网关、通知中心
- `pyin-clients/`：C 端 SDK 分组目录，按 `pyin-client-core`、`pyin-client-config`、`pyin-client-dict`、`pyin-client-bom-group` 划分
- `pyin-apps/`：独立前端应用，目前包含 `pyin-apps/pyin-web-shell`
- `pyin-plugins/`：插件源码模块目录，直接放每个插件模块，例如 `pyin-plugins/pyin-plugin-config`
- `pyin-distribution-parent/`：发布与装配相关内容，包含系统插件发布产物和最终部署运行目录

## 目录语义

- 源码目录：`pyin-services/`、`pyin-clients/`、`pyin-apps/`、`pyin-plugins/`
- 发布目录：`pyin-distribution-parent/bundled-plugins/`
- 部署运行目录：`pyin-distribution-parent/runtime/pyin-config-center-runtime/`

不要把插件源码放到 `pyin-distribution-parent/bundled-plugins/`，也不要把 `node_modules/`、`dist/`、`target/` 当成仓库结构的一部分。

## 模块概览

- `pyin-services/pyin-bootstrap`：Spring Boot 启动入口
- `pyin-services/pyin-common`：通用返回体、异常、常量、工具
- `pyin-services/pyin-core`：用户、角色、权限、鉴权、插件元数据、接入凭证、审计、系统设置
- `pyin-services/pyin-plugin-platform/*`：插件 SPI 与插件开发 SDK 分组模块，包含 `pyin-plugin-spi`、`pyin-plugin-sdk-core`、`pyin-plugin-sdk-embedded`、`pyin-plugin-sdk-standalone`
- `pyin-services/pyin-plugin-runtime` / `pyin-services/pyin-plugin-gateway` / `pyin-services/pyin-notify`：插件运行时、网关与通知中心
- `pyin-clients/pyin-client-core/pyin-client-*`：C 端公共接口与核心 SDK
- `pyin-clients/pyin-client-config/pyin-client-*`：配置功能 SDK 与 Starter
- `pyin-clients/pyin-client-dict/pyin-client-*`：字典功能 SDK 与 Starter
- `pyin-clients/pyin-client-bom-group/pyin-client-bom`：客户端 BOM
- `pyin-apps/pyin-web-shell`：Vue 3 前端壳
- `pyin-plugins/pyin-plugin-config` / `pyin-plugins/pyin-plugin-dict`：插件源码模块
- `pyin-distribution-parent/pyin-distribution`：运行目录装配定义

## 快速开始

### 后端构建

```bash
mvn clean install -DskipTests
```

### 前端壳构建

```bash
cd pyin-apps/pyin-web-shell
npm install
npm run build
```

### 系统插件前端构建

```bash
cd pyin-plugins/pyin-plugin-config/frontend
npm install
npm run build

cd ../../pyin-plugin-dict/frontend
npm install
npm run build
```

### 插件编译联动

系统插件后端在 Maven 编译时会自动执行对应 `frontend/` 的 `npm ci` 与 `npm run build`，并将构建结果复制到各自后端的 `src/main/resources/plugin-static/<pluginId>/` 目录中。

### 单插件 Maven 构建

插件后端依赖当前仓库中的 SPI 与 SDK 快照。构建单个插件时，必须从仓库根目录通过 Maven 反应堆同时构建这些依赖，避免使用本地仓库中过期的快照 JAR：

```bash
mvn -pl pyin-plugins/pyin-plugin-dict/backend -am -DskipTests package
```

仅验证 Java 编译、且前端产物已存在时，可跳过前端构建：

```bash
mvn -pl pyin-plugins/pyin-plugin-dict/backend -am -DskipTests -Dexec.skip=true clean compile
```

不要直接在 `pyin-plugins/pyin-plugin-dict/` 目录执行 Maven 编译来获取最新 SPI/SDK；若确实需要这样做，应先在仓库根目录执行 `mvn clean install -DskipTests` 安装当前快照依赖。

当前插件开发模型采用 Java-only `PluginManifest`，插件通过 `PyinPlugin.manifest()` 声明基础信息；平台不再提供 `menus()` 或插件菜单树。主前端壳点击插件后固定跳转 `/plugins/{pluginId}`，并动态加载前端联邦暴露的 `src/exposed/routes.ts`；接口与权限默认由 `@AdminApi` / `@ClientSdkApi` 自动扫描生成，不再要求手写 `plugin.yml`。完整规则见 [插件工作区路由规范](docs/plugin-workspace-routing.md)。

当前默认系统插件的模块联邦产物会把 `remoteEntry.js` 放在 `plugin-static/<pluginId>/assets/` 下；网关继续对外暴露 `/plugin-static/{pluginId}/remoteEntry.js`，并兼容该入口衍生出的根级相对 `js/css` 资源请求，将其回退到 `assets/` 目录解析。

## 架构约束

- 字典与配置业务必须作为系统插件实现，不能进入 `pyin-services/pyin-core`
- C 端认证仅支持 `accessKey/accessSecret`
- 核心 SDK 负责 Token、HTTP、通知连接与事件分发
- 功能 SDK 只通过 JVM 内部接口接收事件
