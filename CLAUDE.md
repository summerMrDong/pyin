# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 项目概述

Pyin（pyin-config-center）是一个插件化配置中心平台。核心平台提供用户/角色/权限管理、登录鉴权、插件运行时、请求网关、通知中心、接入凭证管理等能力。字典、配置等业务功能以插件形式实现，**禁止**写入 `pyin-core`。

**技术栈：** Spring Boot 3.3 + Java 17 + Sa-Token + MyBatis-Plus | Vue 3 + Vite + 模块联邦（@originjs/vite-plugin-federation）

**数据库：** 本地开发使用 H2（file 模式，MySQL 兼容），生产环境接入 MySQL 8。

## 常用命令

```bash
# 全量后端构建（跳过测试）
mvn clean install -DskipTests

# 构建单个后端模块
mvn clean install -DskipTests -pl pyin-services/pyin-core

# 启动应用（项目根目录执行）
mvn spring-boot:run -pl pyin-services/pyin-bootstrap

# 前端壳开发
cd pyin-apps/pyin-web-shell && npm install && npm run dev    # 开发服务器，端口 5173
cd pyin-apps/pyin-web-shell && npm run build

# 插件前端构建（每个插件有独立的 frontend/）
cd pyin-plugins/pyin-plugin-config/frontend && npm install && npm run build
cd pyin-plugins/pyin-plugin-dict/frontend && npm install && npm run build

# 插件后端在 Maven 编译时会自动执行对应 frontend/ 的 npm ci + npm run build
```

**运行测试：** 测试代码位于 `pyin-services/pyin-bootstrap/src/test/`（集成测试级别，使用 spring-boot-starter-test）：
```bash
mvn test -pl pyin-services/pyin-bootstrap
```

开发服务器启动端口为 **8080**，使用 H2 数据库。前端壳的 Vite 开发服务器将 `/api`、`/capi`、`/plugin-static` 代理到 `http://127.0.0.1:8080`。

## 架构

### 三层结构

```
核心平台 (pyin-services/)
├── pyin-bootstrap        — Spring Boot 启动入口 (PyinConfigCenterApplication)
├── pyin-common           — Result、ErrorCode、BusinessException、工具类（无业务逻辑）
├── pyin-core             — 用户/角色/权限/鉴权/插件元数据/接入凭证/审计/系统设置
├── pyin-plugin-platform/ — 插件 SPI 与 SDK
│   ├── pyin-plugin-spi         — PyinPlugin 接口、PluginManifest、PluginContext
│   ├── pyin-plugin-sdk-core    — @AdminApi、@ClientSdkApi、@InternalApi、清单装配、签名校验
│   ├── pyin-plugin-sdk-embedded — 内嵌插件支持
│   └── pyin-plugin-sdk-standalone — 独立插件支持
├── pyin-plugin-runtime   — 插件生命周期：扫描、加载、安装、启动、停止、卸载
├── pyin-plugin-gateway   — 请求转发（管理端: /api/plugins/{pluginId}/admin/**，C端SDK: /capi/plugins/{pluginId}/client/**）
└── pyin-notify           — 事件总线、SSE/WebSocket 推送 (/capi/events/stream)

客户端 SDK (pyin-clients/)
├── pyin-client-core/     — 认证(accessKey/accessSecret)、Token、HTTP客户端、事件通道、功能注册表
├── pyin-client-config/   — 配置功能 SDK + Spring Starter
├── pyin-client-dict/     — 字典功能 SDK + Spring Starter
└── pyin-client-bom/      — 版本管理 BOM

前端壳 (pyin-apps/pyin-web-shell/)
└── Vue 3 SPA，通过模块联邦动态加载插件页面

插件 (pyin-plugins/)
├── pyin-plugin-config/   — 系统插件（配置管理）
├── pyin-plugin-dict/     — 系统插件（字典管理）
└── pyin-plugin-file/     — 外部插件（文件管理）
```

### 请求流程

- **管理端请求：** 浏览器 → pyin-web-shell → `/api/plugins/{pluginId}/admin/**` → pyin-plugin-gateway → 插件后端
- **C端 SDK 请求：** 外部应用 → pyin-client-core-sdk → `/capi/plugins/{pluginId}/client/**` → pyin-plugin-gateway → 插件后端
- **事件通知：** 插件发布事件 → pyin-notify → SSE/WebSocket → pyin-client-core-sdk → 通过 JVM 内部 Java 方法调用分发给已注册的功能 SDK（非 HTTP）

## 核心约束

以下规则来自 AGENTS.md，优先级高于其他任何指导。如有冲突，信任顺序：(1) 当前 SPI/SDK Java 源码，(2) AGENTS.md 顶部补充规则，(3) AGENTS.md AI 插件开发专项约束。

### 领域边界

- 业务功能（字典、配置、公告、流程、表单等）**必须做成插件**，放在 `pyin-plugins/` 下，**禁止**写入 `pyin-services/pyin-core/`。
- 核心平台**只允许**包含：用户、角色、权限、鉴权、插件元数据、接入凭证、审计、系统设置。

### 插件开发

- 插件入口：实现 `spi.com.pyin.PyinPlugin`，通过 `manifest()` 返回 `PluginManifest` 提供元数据。
- `plugin.yml` 已**退役**，不得生成、复制或依赖。
- 插件源码结构：`pyin-plugins/pyin-plugin-<name>/backend/` + `frontend/`
- 插件后端注解：管理端用 `@PluginAdminController` + `@AdminApi`，C端用 `@PluginClientController` + `@ClientSdkApi`。
- 接口路径和 HTTP 方法由 Spring MVC 注解（`@GetMapping`、`@PostMapping` 等）提供，**不要**另外维护一套字符串接口清单。
- 权限编码格式：`<pluginId>:<action>`（如 `dict:view`、`config:create`）。
- 插件前端**必须**使用模块联邦，在 `frontend/src/exposed/` 下暴露 `./<PluginRemoteApp>.vue`、`./routes.ts`。
- 插件菜单必须由后端 `PyinPlugin.menus()` 显式提供；页面路由仅由前端联邦 `routes.ts` 提供，后端不维护 `routes` 元数据。
- `manifest().remoteName` 必须与模块联邦 `name` 一致；`manifest().exposedModule` 必须与 `exposes` 的主模块名一致。
- 默认开发顺序：骨架 → manifest → 控制器/接口/权限 → 后端 menus + 前端 exposed/routes → 业务实现 → 打包。

### 客户端 SDK 规则

- C端认证仅支持 `accessKey`/`accessSecret`，不使用 appId/appSecret，不做用户登录。
- 核心 SDK（`pyin-client-core-sdk`）统一管理 Token、HTTP、SSE、事件分发。所有自动装配 Bean 必须使用 `@ConditionalOnMissingBean`。
- 功能 SDK 通过 JVM 内部 Java 接口调用接收核心 SDK 分发的事件，**不是**通过 HTTP。
- 功能 SDK 可通过核心 SDK 的 `HttpClient` 回调配置中心。
- Spring Starter 命名：`pyin-client-<feature>-spring-starter`（如 `pyin-client-config-spring-starter`）。

### 打包

- 插件发布单元：zip 包，包含 `plugin-backend.jar` + `web/remoteEntry.js` + `web/assets/`。
- 系统插件发布到 `pyin-distribution-parent/bundled-plugins/`。
- 外部插件部署到 `pyin-distribution-parent/runtime/pyin-config-center-runtime/plugins/external/`。
- 系统插件和外部插件走同一套加载机制。

## 命名规范

| 模块 | artifactId |
|------|-----------|
| 启动入口 | `pyin-bootstrap` |
| 通用模块 | `pyin-common` |
| 核心平台 | `pyin-core` |
| 插件 SPI | `pyin-plugin-spi` |
| 插件 SDK | `pyin-plugin-sdk-core` |
| 插件运行时 | `pyin-plugin-runtime` |
| 插件网关 | `pyin-plugin-gateway` |
| 通知中心 | `pyin-notify` |
| 前端壳 | `pyin-web-shell` |
| 客户端功能 Starter | `pyin-client-<feature>-spring-starter` |

## 关键路径

- Java 包名前缀：`com.pyin.center`（服务端）、`com.pyin.plugin`（插件）
- 启动类：`bootstrap.com.pyin.PyinConfigCenterApplication`
- 应用配置文件：`pyin-services/pyin-bootstrap/src/main/resources/application.yml`
- H2 数据库文件：`pyin-distribution-parent/runtime/pyin-config-center-runtime/data/`
- AGENTS.md：权威 AI 编码规范，进行重要改动前必读
- `docs/`：architecture.md、plugin-dev-guide.md、sdk-dev-guide.md、client-sdk-spec.md、gateway-design.md、notify-design.md

## 文档与需求

`docs/` 和 `inputs/` 目录包含业务需求、原型和设计文档。开发业务插件时，参考 `inputs/requirements/main/` 获取业务上下文，参考 `inputs/prototypes/` 获取 HTML 原型。
