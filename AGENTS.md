# AGENTS.md

本文件是 **Pyin 配置中心 P1** 项目的 AI 编程代理开发规范。

所有 AI Agent、Codex、Claude Code、Cursor、Copilot Workspace 等工具在生成、修改、重构代码时，必须优先遵守本文档。

> 2026-06 SDK 升级补充规则：
> 1. 插件开发相关模块统一收拢在 `pyin-services/pyin-plugin-platform/` 下。
> 2. 插件元数据唯一事实来源为 Java `PluginManifest` 与 `PyinPlugin.manifest()`。
> 3. `plugin.yml` 已从当前实现中退役；若本文后续旧章节与此冲突，以本补充规则为准。

## 0.1 AI 插件开发专项约束

本节是 AI 在 Pyin 中开发插件时的最高优先级落地规则。

若与本文后续旧章节、旧示例、旧打包描述冲突，以本节和顶部 2026-06 补充规则为准。

### 开发前判断

AI 在开始写代码前，必须先判断需求属于核心域还是插件域。

必须做成插件的能力：

```text
字典
配置
公告
流程
行业业务
具体业务表单
其他可独立装配的业务能力
```

必须留在核心平台的能力：

```text
用户
角色
权限
鉴权
接入凭证
审计
系统设置
插件元数据管理
插件运行时管理
```

禁止把业务插件能力直接写入 `pyin-services/pyin-core/`。

### 目录与模块约束

新插件源码只能放在：

```text
pyin-plugins/pyin-plugin-<name>/
```

插件源码目录最少包含：

```text
backend/
frontend/
```

AI 必须区分以下目录语义：

```text
pyin-plugins/                           插件源码目录
pyin-distribution-parent/bundled-plugins/  系统插件发布产物目录
pyin-distribution-parent/runtime/          运行时部署目录
frontend/dist/                             前端临时构建产物，不是源码目录
target/                                    Maven 构建产物，不是源码目录
```

禁止把 `bundled-plugins/`、`runtime/`、`dist/`、`target/` 当成插件源码目录进行开发。

插件运行来源由平台加载入口决定：

```text
PluginSourceType.EMBEDDED_SYSTEM  仅限中心 classpath 内且位于 embedded-plugin-ids 白名单的系统插件
PluginSourceType.STANDALONE_NODE   独立插件进程通过节点注册协议接入中心
```

不得在 Manifest 中声明或恢复 `pluginType`、`runtimeMode`。外部插件不得进入中心 classpath；未通过平台
白名单的 `PyinPlugin` Bean 不得注册为内嵌插件。

### 插件公共契约/API 模块约束

插件只有在存在真实跨模块、跨插件公共能力时，才新增 `api` Maven 子模块；不得为空能力提前创建空 `api` 模块。

插件公共能力统一采用：

```text
pyin-plugins/pyin-plugin-<name>/api
artifactId：pyin-plugin-<name>-api
package：com.pyin.plugin.<name>.api
```

`api` 模块必须只放稳定契约、DTO/View、少量确有必要的公共工具，不得依赖插件 `backend`，不得放业务实现、Controller、Repository、Entity 持久化对象。

`api` 包结构必须按职责分层：

```text
api/service    公共服务接口契约
api/model      跨模块传输 DTO / View / Identity
api/support    公共工具、常量、辅助类型；没有真实工具时不得创建空包
```

所有 `*-api`、`*-spi`、`*-sdk` 等契约模块中的 public 接口、record、DTO、View、Identity、异常、枚举，都必须编写详细中文 JavaDoc。

JavaDoc 必须满足：

```text
1. public 类型必须说明职责边界、适用调用方、是否允许跨模块或跨插件调用。
2. public 接口的每个方法必须说明用途、参数、返回值、空值约定、异常或失败语义。
3. record / DTO / View / Identity 必须说明模型用途，并用 @param 说明每个字段含义。
4. 涉及密码、Secret、Token、签名、权限、用户身份等敏感字段，必须说明安全使用边界。
5. deprecated 契约必须说明废弃原因和推荐替代契约。
6. 不允许在契约模块中留下无注释的 public 类型或方法。
```

AI 在提交契约/API 模块改动前，必须自查所有新增或修改的 public 类型和方法是否具备中文 JavaDoc；缺失时不得认为任务完成。

### 后端实现约束

插件入口类必须实现：

```java
spi.com.pyin.PyinPlugin
```

插件元数据必须通过：

```java
PyinPlugin.manifest()
```

返回 Java `PluginManifest`，不得再生成、复制或依赖 `plugin.yml`。

`manifest()` 默认只填写基础字段：

```text
pluginId
pluginName
pluginVersion
basePath
entryJs
```

独立插件的后端地址、前端地址、健康检查地址由其节点注册协议提交；运行来源由平台写入
`PluginSourceType`，不属于插件 Manifest。

API、权限、路由、资源默认遵循“自动扫描 + 自动装配”：

```text
后端接口与权限：优先由 @AdminApi / @ClientSdkApi / @InternalApi 扫描生成
前端路由：优先由 frontend 模块联邦暴露的 routes.ts 提供
resources：优先由装配器自动生成
```

`pluginId` 是 `PluginManifest` 的唯一事实来源。插件入口不得实现或新增 `PyinPlugin.pluginId()`；
必须使用 `PluginManifest.builder("<pluginId>")` 创建清单，构建器不提供无参入口，也不再提供
重复设置 ID 的链式方法。

除非确有必要，AI 不应手工覆盖 `permissions`、`apis`、`resources`。

控制器约束：

```text
管理端控制器：@PluginAdminController
C端控制器：@PluginClientController
管理端接口：@AdminApi
C端接口：@ClientSdkApi
内部接口：@InternalApi
```

接口真实路径与 HTTP 方法必须由 Spring MVC 注解提供，例如：

```text
@GetMapping
@PostMapping
@PutMapping
@DeleteMapping
```

AI 不要再手工维护一套和源码脱节的字符串接口清单。

权限编码默认遵循：

```text
<pluginId>:<action>
```

例如：

```text
notice:view
notice:create
notice:update
notice:delete
```

### 前端实现约束

插件前端统一采用：

```text
Vue 3 + Vite + 模块联邦
```

前端最小暴露结构：

```text
frontend/src/exposed/
├── <PluginRemoteApp>.vue
└── routes.ts
```

工作区路由约束：

```text
主前端壳只展示插件工作区标签；标签点击后固定跳转 /plugins/{pluginId}
每个插件必须通过模块联邦暴露 ./routes，且 routes.ts 必须声明精确的 /plugins/{pluginId} 入口路由
routes.ts 是插件页面与内部导航的唯一事实来源；壳应用动态注册该路由，不推导或补充页面路由
插件内部子路由必须位于 /plugins/{pluginId}/**，由插件自行决定是否使用菜单、页签、树或单页布局
PyinPlugin 不再提供 menus()；不得新增 PluginMenu、menus.ts、routes.json 或后端菜单树
```

一致性约束：

```text
模块联邦 name                  必须等于 pluginId；壳应用据此加载远端
entryJs                        必须与运行时暴露的 remoteEntry.js 路径一致
./routes                       必须在 exposes 中；插件可额外暴露任意组件、组合式函数或前端能力供其他插件引用
```

### 打包与运行约束

插件发布单元是插件包目录或 zip，不是源码目录。

发布产物结构按当前实现应理解为：

```text
plugin-package/
├── plugin-backend.jar
└── web/
    ├── remoteEntry.js
    └── assets/
```

AI 不得在新实现中继续生成以下旧模型内容：

```text
plugin.yml
手写 plugin.yml 清单复制步骤
把 plugin.yml 当作部署必需文件
```

运行来源判断：

```text
EMBEDDED_SYSTEM  中心从受控 classpath 装配，且 pluginId 必须在 embedded-plugin-ids 白名单中
STANDALONE_NODE   插件独立启动并通过节点注册协议向中心注册
```

插件作者不得通过 Manifest 选择运行来源；来源不明确或不符合部署策略时，平台必须拒绝注册。

### AI 工作流程与交付要求

开发插件时，AI 必须优先参考现有样例：

```text
pyin-plugins/pyin-plugin-config
pyin-plugins/pyin-plugin-dict
pyin-plugins/pyin-plugin-file
```

默认开发顺序：

```text
1. 先补最小可运行插件骨架
2. 再补 manifest 基础字段
3. 再补控制器、接口与权限
4. 再补前端 exposed、routes 与插件内部导航
5. 最后补业务实现、打包与验证
```

禁止一开始就发散式抽象出大量尚未落地的通用层。

若发现文档与代码冲突，优先信任以下事实来源：

```text
1. 当前 SPI/SDK Java 代码
2. 本文件顶部 2026-06 补充规则
3. 本节 AI 插件开发专项约束
4. 其他历史章节
```

AI 交付插件相关改动时，必须明确说明：

```text
新增了哪些接口
新增了哪些权限
新增了哪些前端路由与插件内部导航方式
采用了哪种运行模式
打包入口和前端 remoteEntry.js 如何对应
```

---

# 1. 项目基本信息

## 1.1 项目名称

```text
pyin-config-center
```

## 1.2 产品名称

```text
Pyin 配置中心
```

## 1.3 Maven groupId

```xml
<groupId>com.pyin</groupId>
```

## 1.4 Java 包名前缀

```text
com.pyin.center
com.pyin.plugin
```

## 1.5 启动 Jar

```text
pyin-config-center.jar
```

## 1.6 运行目录

```text
pyin-distribution-parent/runtime/pyin-config-center-runtime/
```

---

# 2. P1 核心定位

Pyin P1 是一个插件化配置中心平台。

Pyin 配置中心本体只负责平台内核能力，不直接实现字典管理和配置管理业务。

## 2.1 Pyin 核心负责

```text
1. 用户管理
2. 角色管理
3. 权限管理
4. 登录鉴权
5. 插件运行时
6. 插件安装、加载、启停、卸载、升级
7. 插件状态监听
8. 插件请求转发网关
9. 插件事件接收
10. 配置变更与事件通知
11. C端 SDK 接入凭证管理
12. 操作审计
13. 系统设置
```

## 2.2 Pyin 核心不负责

```text
1. 字典类型管理
2. 字典项管理
3. 配置项管理
4. 配置发布
5. 配置版本管理
6. 公告管理
7. 工作流管理
8. 行业业务功能
9. 具体业务表单
```

这些能力必须通过插件实现。

---

# 3. 核心架构原则

## 3.1 核心不写死业务

禁止在 `pyin-core` 中实现字典、配置、公告、流程等具体业务。

允许在 `pyin-core` 中实现：

```text
用户
角色
权限
鉴权
插件元数据
插件状态
接入凭证
审计
系统设置
```

不允许在 `pyin-core` 中实现：

```text
字典业务
配置业务
公告业务
流程业务
行业业务
```

---

## 3.2 字典和配置是默认系统插件

字典管理和配置管理不是核心模块，而是默认系统插件。

```text
dict-plugin      字典管理插件
config-plugin    配置管理插件
```

它们和普通插件一样，都必须包含：

```text
Java PluginManifest
backend jar
frontend remoteEntry.js
web assets
权限
路由
API
事件
```

区别只是：

```text
普通插件：运行时投放到 pyin-distribution-parent/runtime/pyin-config-center-runtime/plugins/external/
默认插件：发布时进入 pyin-distribution-parent/bundled-plugins/
```

启动时，Pyin 自动把默认系统插件解压到：

```text
pyin-distribution-parent/runtime/pyin-config-center-runtime/plugins/system/
```

然后按统一插件运行时机制加载。

---

## 3.3 系统插件和外部插件走同一套机制

系统插件：

```text
pyin-plugins/pyin-plugin-dict
pyin-plugins/pyin-plugin-config
```

外部插件：

```text
pyin-plugins/pyin-plugin-*
```

源码都必须遵守同一套插件生命周期、打包、加载、启停、前端模块联邦和后端接口规范。

发布后的系统插件进入：

```text
pyin-distribution-parent/bundled-plugins/
```

运行时外部插件投放目录：

```text
pyin-distribution-parent/runtime/pyin-config-center-runtime/plugins/external/
```

---

## 3.4 C端 SDK 采用“核心 SDK + 功能 SDK”模式

C端不是终端用户登录模式，也不是 appId/appSecret 开放平台模式。

C端是业务系统通过 SDK 接入 Pyin 配置中心。

C端 SDK 分为：

```text
核心 SDK：
认证、Token、HTTP、通知通道、事件分发。

功能 SDK：
配置、字典、公告、流程等具体能力。
```

用户不需要单独引用核心 SDK。

用户想用哪个能力，就引入哪个功能 SDK。

例如，只使用配置能力：

```xml
<dependency>
    <groupId>com.pyin</groupId>
    <artifactId>pyin-client-config-spring-starter</artifactId>
    <version>1.0.0</version>
</dependency>
```

该功能 SDK Starter 必须传递依赖核心 SDK Starter。

核心 SDK 收到配置中心通知后，不通过 HTTP 调用功能 SDK，而是通过 JVM 内部 Java 类接口调用功能 SDK。

```text
配置中心
  ↓ SSE / WebSocket
pyin-client-core-sdk
  ↓ Java 类接口调用
pyin-client-config-sdk / pyin-client-dict-sdk / 其他功能 SDK
```

---

# 4. Maven artifactId 命名规范

命名原则：

```text
1. 不要过度精简。
2. 必须见名知意。
3. 可以省略 boot，但保留 spring-starter。
4. 服务端模块应体现 plugin、gateway、runtime、web-shell 等语义。
5. C端模块应体现 client、core、config、dict、sdk、spring-starter 等语义。
```

---

## 4.1 服务端模块命名

| 模块职责     | artifactId            |
| -------- | --------------------- |
| 启动入口     | `pyin-bootstrap`      |
| 通用模块     | `pyin-common`         |
| 核心平台能力   | `pyin-core`           |
| 插件 SPI   | `pyin-plugin-spi`     |
| 插件开发 SDK | `pyin-plugin-sdk-core` |
| 插件运行时    | `pyin-plugin-runtime` |
| 插件网关     | `pyin-plugin-gateway` |
| 通知中心     | `pyin-notify`         |
| 主前端壳     | `pyin-web-shell`      |
| 部署组装     | `pyin-distribution`   |

允许：

```text
pyin-plugin-runtime
pyin-plugin-gateway
pyin-web-shell
pyin-distribution
```

不推荐：

```text
pyin-runtime
pyin-gateway
pyin-web
pyin-dist
```

原因：过度精简后不够见名知意。

---

## 4.2 C端 SDK 模块命名

| 模块职责                | artifactId                          |
| ------------------- | ----------------------------------- |
| C端公共接口              | `pyin-client-api`                   |
| C端核心 SDK            | `pyin-client-core-sdk`              |
| C端核心 Spring Starter | `pyin-client-core-spring-starter`   |
| 配置功能 SDK            | `pyin-client-config-sdk`            |
| 配置功能 Spring Starter | `pyin-client-config-spring-starter` |
| 字典功能 SDK            | `pyin-client-dict-sdk`              |
| 字典功能 Spring Starter | `pyin-client-dict-spring-starter`   |
| C端版本管理 BOM          | `pyin-client-bom`                   |

推荐：

```xml
<artifactId>pyin-client-config-spring-starter</artifactId>
<artifactId>pyin-client-dict-spring-starter</artifactId>
<artifactId>pyin-client-core-spring-starter</artifactId>
```

不推荐：

```xml
<artifactId>pyin-client-config-spring-boot-starter</artifactId>
<artifactId>pyin-client-dict-spring-boot-starter</artifactId>
<artifactId>pyin-client-core-spring-boot-starter</artifactId>
```

原因：名称过长。

也不推荐：

```xml
<artifactId>pyin-config-starter</artifactId>
<artifactId>pyin-dict-starter</artifactId>
<artifactId>pyin-client-config-starter</artifactId>
```

原因：过度精简后看不出是否为 Spring Starter，语义不够完整。

最终采用：

```text
pyin-client-config-spring-starter
pyin-client-dict-spring-starter
pyin-client-core-spring-starter
```

---

## 4.3 插件模块命名

系统插件 artifactId：

```text
pyin-plugin-dict
pyin-plugin-config
```

外部插件 artifactId：

```text
pyin-plugin-notice
pyin-plugin-workflow
pyin-plugin-xxx
```

插件源码模块目录推荐使用：

```text
pyin-plugin-dict/
pyin-plugin-config/
pyin-plugin-notice/
pyin-plugin-workflow/
```

但 Maven artifactId 推荐使用：

```text
pyin-plugin-dict
pyin-plugin-config
pyin-plugin-notice
```

---

# 5. 推荐项目结构

```text
pyin-config-center/
├── pom.xml
├── AGENTS.md
├── README.md
├── docs/
│   ├── architecture.md
│   ├── plugin-dev-guide.md
│   ├── sdk-dev-guide.md
│   ├── plugin-package-spec.md
│   ├── client-sdk-spec.md
│   ├── gateway-design.md
│   └── notify-design.md
│
├── pyin-services/
│   ├── pyin-bootstrap/
│   ├── pyin-common/
│   ├── pyin-core/
│   ├── pyin-plugin-platform/
│   │   ├── pyin-plugin-spi/
│   │   ├── pyin-plugin-sdk-core/
│   │   ├── pyin-plugin-sdk-embedded/
│   │   └── pyin-plugin-sdk-standalone/
│   ├── pyin-plugin-runtime/
│   ├── pyin-plugin-gateway/
│   └── pyin-notify/
├── pyin-clients/
│   ├── pyin-client-core/
│   │   ├── pyin-client-api/
│   │   ├── pyin-client-core-sdk/
│   │   └── pyin-client-core-spring-starter/
│   ├── pyin-client-config/
│   │   ├── pyin-client-config-sdk/
│   │   └── pyin-client-config-spring-starter/
│   ├── pyin-client-dict/
│   │   ├── pyin-client-dict-sdk/
│   │   └── pyin-client-dict-spring-starter/
│   └── pyin-client-bom-group/
│       └── pyin-client-bom/
├── pyin-apps/
│   └── pyin-web-shell/
├── pyin-distribution-parent/
│   ├── pyin-distribution/
│   ├── bundled-plugins/
│   └── runtime/
│       └── pyin-config-center-runtime/
└── pyin-plugins/
    ├── pyin-plugin-config/
    └── pyin-plugin-dict/
```

---

# 6. 模块职责

## 6.1 pyin-bootstrap

唯一启动入口。

启动类：

```java
package com.pyin.center.bootstrap;

public class PyinConfigCenterApplication {

    public static void main(String[] args) {
        SpringApplication.run(PyinConfigCenterApplication.class, args);
    }
}
```

启动命令：

```bash
java -jar pyin-config-center.jar
```

---

## 6.2 pyin-common

通用基础模块。

可包含：

```text
统一返回对象
业务异常
错误码
常量
枚举
工具类
JSON 工具
日期工具
加解密工具
```

禁止放入业务逻辑。

---

## 6.3 pyin-core

平台核心模块。

只放：

```text
用户管理
角色管理
权限管理
登录鉴权
插件元数据
插件状态
接入凭证管理
操作审计
系统设置
```

禁止放：

```text
字典业务
配置业务
公告业务
流程业务
行业业务
```

推荐包结构：

```text
pyin-core/
└── src/main/java/com/pyin/center/core/
    ├── user/
    ├── role/
    ├── permission/
    ├── auth/
    ├── plugin/
    ├── clientcredential/
    ├── audit/
    └── system/
```

---

## 6.4 pyin-plugin-spi

插件 SPI 接口模块。

用于定义配置中心和插件共同依赖的接口与模型。

可包含：

```text
PyinPlugin
PluginContext
PluginLifecycle
PluginDescriptor
PluginAccessMode
PluginPermissionProvider
PluginApiDefinition
PluginEvent
PluginEventPublisher
```

插件 SPI 不包含 `PluginMenu`、`PluginMenuProvider` 或其他平台级菜单契约；插件页面路由由前端
`./routes` 模块声明。

---

## 6.5 pyin-plugin-sdk-core

插件开发 SDK。

给插件开发者使用。

负责：

```text
插件接口注解
插件 API 声明
插件请求签名校验
插件上下文获取
插件事件发布
插件生命周期辅助
```

插件后端必须依赖：

```xml
<dependency>
    <groupId>com.pyin</groupId>
    <artifactId>pyin-plugin-spi</artifactId>
    <version>1.0.0</version>
</dependency>

<dependency>
    <groupId>com.pyin</groupId>
    <artifactId>pyin-plugin-sdk-core</artifactId>
    <version>1.0.0</version>
</dependency>
```

内嵌插件通常额外依赖：

```xml
<dependency>
    <groupId>com.pyin</groupId>
    <artifactId>pyin-plugin-sdk-embedded</artifactId>
    <version>1.0.0</version>
</dependency>
```

独立插件通常额外依赖：

```xml
<dependency>
    <groupId>com.pyin</groupId>
    <artifactId>pyin-plugin-sdk-standalone</artifactId>
    <version>1.0.0</version>
</dependency>
```

---

## 6.6 pyin-plugin-runtime

插件运行时模块。

负责：

```text
插件扫描
默认插件解压
外部插件加载
插件安装
插件启动
插件停止
插件卸载
插件升级
插件状态监听
插件健康检查
插件工作区注册信息维护
插件权限注册
插件 API 注册
```

推荐包结构：

```text
pyin-plugin-runtime/
└── src/main/java/com/pyin/center/runtime/
    ├── manager/
    ├── loader/
    ├── registry/
    ├── state/
    └── event/
```

---

## 6.7 pyin-plugin-gateway

插件网关模块。

负责请求转发。

包含两条链路：

```text
管理端网关：
/plugins/{pluginId}/admin/**

C端 SDK 网关：
/plugins/{pluginId}/open/**
```

管理端网关使用后台用户鉴权。

C端 SDK 网关使用接入凭证 Token 鉴权。

推荐包结构：

```text
pyin-plugin-gateway/
└── src/main/java/com/pyin/center/gateway/
    ├── admin/
    ├── client/
    ├── dispatcher/
    ├── signature/
    └── staticresource/
```

---

## 6.8 pyin-notify

通知中心模块。

负责：

```text
插件事件接收
配置变更事件处理
字典变更事件处理
插件状态事件处理
C端 SDK 通知连接管理
SSE / WebSocket 推送
```

C端 SDK 统一事件通道：

```text
GET /open/events/stream
```

---

## 6.9 pyin-web-shell

Pyin 主前端壳。

负责：

```text
登录页
首页布局
用户管理
角色管理
权限管理
插件管理
接入凭证管理
系统设置
动态加载插件工作区标签
动态加载插件页面路由
```

插件页面通过模块联邦加载。

---

# 7. C端 SDK 规范

## 7.1 C端 SDK 总体原则

C端 SDK 采用：

```text
核心 SDK + 功能 SDK
```

核心 SDK 负责：

```text
1. 读取 Pyin 地址
2. 使用 accessKey / accessSecret 认证
3. 获取和刷新 Token
4. 封装 HTTP Client
5. 建立 SSE / WebSocket 通知通道
6. 接收中心事件
7. 维护功能 SDK 注册表
8. 将事件分发给功能 SDK
```

功能 SDK 负责：

```text
1. 实现具体能力
2. 注册 featureCode
3. 声明 supportedEventTypes
4. 处理核心 SDK 分发的事件
5. 更新本地缓存
6. 触发业务监听器
7. 必要时通过核心 SDK 的 HttpClient 请求 Pyin
```

---

## 7.2 C端 SDK 依赖规则

用户不需要单独引用核心 SDK。

功能 Spring Starter 必须传递依赖核心 Spring Starter。

例如：

```text
pyin-client-config-spring-starter
├── pyin-client-config-sdk
├── pyin-client-core-spring-starter
├── pyin-client-core-sdk
└── pyin-client-api
```

禁止：

```text
把 pyin-client-core-sdk 打进每个功能 SDK 的 fat jar
```

必须：

```text
通过 Maven 普通依赖传递
```

---

## 7.3 用户引用方式

用户只用配置能力：

```xml
<dependency>
    <groupId>com.pyin</groupId>
    <artifactId>pyin-client-config-spring-starter</artifactId>
    <version>1.0.0</version>
</dependency>
```

用户只用字典能力：

```xml
<dependency>
    <groupId>com.pyin</groupId>
    <artifactId>pyin-client-dict-spring-starter</artifactId>
    <version>1.0.0</version>
</dependency>
```

用户同时使用配置和字典：

```xml
<dependency>
    <groupId>com.pyin</groupId>
    <artifactId>pyin-client-config-spring-starter</artifactId>
</dependency>

<dependency>
    <groupId>com.pyin</groupId>
    <artifactId>pyin-client-dict-spring-starter</artifactId>
</dependency>
```

推荐使用 BOM 统一版本：

```xml
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>com.pyin</groupId>
            <artifactId>pyin-client-bom</artifactId>
            <version>1.0.0</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>
```

---

## 7.4 C端配置规范

```yaml
pyin:
  center:
    enabled: true
    server-url: http://pyin-config-center:8080

    auth:
      mode: ACCESS_KEY
      access-key: cck_xxxxx
      access-secret: ccs_xxxxx

    notify:
      enabled: true
      mode: SSE

    config:
      namespace: ${spring.application.name}
      env: ${spring.profiles.active}
      cache-enabled: true
      watch-enabled: true
```

说明：

```text
server-url      Pyin 配置中心地址
access-key      Pyin 后台生成的接入 Key
access-secret   Pyin 后台生成的接入 Secret
namespace       配置命名空间，默认 spring.application.name
env             环境，默认 spring.profiles.active
notify.mode     通知模式，默认 SSE
```

---

## 7.5 接入凭证规则

Pyin 后台提供：

```text
接入凭证管理
```

用于生成：

```text
Access Key
Access Secret
```

规则：

```text
1. Access Key 可以再次查看
2. Access Secret 只展示一次
3. Access Secret 加密存储，不明文存储
4. 支持禁用凭证
5. 支持重新生成 Secret
6. 支持调用日志审计
```

P1 第一版不做复杂 C端授权。

不做：

```text
namespace 授权
plugin 授权
scope 授权
C端用户登录
appId/appSecret 模型
```

只做：

```text
C端凭证认证
Token 签发
Token 刷新
请求日志
```

---

## 7.6 C端公共接口

模块：

```text
pyin-client-api
```

功能接口：

```java
public interface PyinClientFeature {

    String featureCode();

    String featureName();

    void initialize(PyinClientFeatureContext context);

    List<String> supportedEventTypes();

    void handleEvent(PyinCenterEvent event);
}
```

上下文接口：

```java
public interface PyinClientFeatureContext {

    PyinCenterHttpClient httpClient();

    PyinClientProperties properties();

    void publishLocalEvent(Object event);
}
```

事件对象：

```java
public class PyinCenterEvent {

    private String eventId;

    private String featureCode;

    private String eventType;

    private String namespace;

    private String env;

    private Long version;

    private Long timestamp;

    private Map<String, Object> payload;
}
```

---

## 7.7 核心 SDK 事件分发

核心 SDK 只负责接收和分发事件。

```java
public class PyinClientFeatureRegistry {

    private final Map<String, PyinClientFeature> featureMap = new ConcurrentHashMap<>();

    public void register(PyinClientFeature feature) {
        featureMap.put(feature.featureCode(), feature);
    }

    public void dispatch(PyinCenterEvent event) {
        PyinClientFeature feature = featureMap.get(event.getFeatureCode());

        if (feature == null) {
            return;
        }

        if (!feature.supportedEventTypes().contains(event.getEventType())) {
            return;
        }

        feature.handleEvent(event);
    }
}
```

核心 SDK 收到事件后：

```text
featureCode = config  -> 调用 ConfigClientFeature.handleEvent(event)
featureCode = dict    -> 调用 DictClientFeature.handleEvent(event)
featureCode = notice  -> 调用 NoticeClientFeature.handleEvent(event)
```

这一步是 JVM 内部 Java 类方法调用，不是 HTTP 请求。

---

## 7.8 核心 SDK 自动装配规则

核心 SDK 所有核心 Bean 必须使用：

```java
@ConditionalOnMissingBean
```

避免用户同时引入多个功能 SDK 后重复初始化。

核心 SDK 只能初始化一次：

```text
TokenManager 一个
HttpClient 一个
NotifyClient 一个
FeatureRegistry 一个
事件通道一个
```

禁止：

```text
每个功能 SDK 自己建立通知连接
每个功能 SDK 自己认证和刷新 Token
每个功能 SDK 自己读取 accessSecret
```

功能 SDK 只能使用核心 SDK 提供的：

```text
PyinCenterHttpClient
PyinClientFeatureContext
PyinClientFeatureRegistry
```

---

## 7.9 功能 SDK 可以请求中心

核心 SDK 到功能 SDK：

```text
Java 类接口调用
```

功能 SDK 到 Pyin：

```text
可以通过核心 SDK 提供的 HttpClient 请求中心
```

例如字典事件：

```json
{
  "featureCode": "dict",
  "eventType": "dict.changed",
  "payload": {
    "typeCode": "gender"
  }
}
```

字典 SDK 收到事件后，可以重新拉取字典：

```java
context.httpClient().get("/plugins/dict/open/dict/items?typeCode=gender");
```

---

# 8. 配置功能 SDK 规范

模块：

```text
pyin-client-config-sdk
pyin-client-config-spring-starter
```

对外接口：

```java
public interface PyinConfigClient {

    String getValue(String key);

    String getValue(String key, String defaultValue);

    String getValue(String namespace, String env, String key, String defaultValue);

    Integer getInt(String key, Integer defaultValue);

    Boolean getBoolean(String key, Boolean defaultValue);

    Map<String, String> getNamespace(String namespace, String env);

    void addListener(ConfigChangedListener listener);
}
```

监听接口：

```java
public interface ConfigChangedListener {

    void onChanged(ConfigChangedEvent event);
}
```

业务使用：

```java
@Autowired
private PyinConfigClient configClient;

String timeout = configClient.getValue("order.timeout", "30");
```

监听配置变化：

```java
@PyinConfigListener(keys = {"order.timeout"})
public void onChanged(ConfigChangedEvent event) {
    // 处理配置变更
}
```

配置 SDK 的 featureCode：

```text
config
```

支持事件：

```text
config.changed
config.deleted
config.refreshed
```

---

# 9. 字典功能 SDK 规范

模块：

```text
pyin-client-dict-sdk
pyin-client-dict-spring-starter
```

对外接口：

```java
public interface PyinDictClient {

    String getLabel(String typeCode, String itemValue);

    List<DictItem> getItems(String typeCode);

    Map<String, String> getDictMap(String typeCode);
}
```

业务使用：

```java
@Autowired
private PyinDictClient dictClient;

String label = dictClient.getLabel("gender", "1");
```

字典 SDK 的 featureCode：

```text
dict
```

支持事件：

```text
dict.changed
dict.refreshed
```

---

# 10. 插件开发规范

## 10.1 插件类型

插件分为：

```text
系统插件：
dict-plugin
config-plugin

外部插件：
notice-plugin
workflow-plugin
其他业务插件
```

所有插件必须遵循统一包结构：

```text
Java PluginManifest
backend jar
frontend remoteEntry.js
web assets
```

最终插件包：

```text
notice-plugin.zip
├── plugin-backend.jar
└── web/
    ├── remoteEntry.js
    └── assets/
```

---

## 10.2 插件源码结构

```text
notice-plugin/
├── pom.xml
├── backend/
│   ├── pom.xml
│   └── src/main/java/com/pyin/plugin/notice/
│       ├── NoticePlugin.java
│       ├── controller/
│       │   ├── NoticeAdminController.java
│       │   └── NoticeClientController.java
│       ├── service/
│       ├── mapper/
│       ├── entity/
│       ├── permission/
│       ├── event/
│       └── migration/
│
└── frontend/
    ├── package.json
    ├── vite.config.ts
    └── src/
        ├── exposed/
        │   ├── NoticeRemoteApp.vue
        │   └── routes.ts
        ├── views/
        └── api/
```

---

## 10.3 插件依赖

插件后端必须依赖：

```xml
<dependency>
    <groupId>com.pyin</groupId>
    <artifactId>pyin-plugin-spi</artifactId>
    <version>1.0.0</version>
</dependency>

<dependency>
    <groupId>com.pyin</groupId>
    <artifactId>pyin-plugin-sdk-core</artifactId>
    <version>1.0.0</version>
</dependency>
```

---

## 10.4 插件入口类

```java
@PluginComponent
public class NoticePlugin implements PyinPlugin {

    @Override
    public PluginManifest manifest() {
        return PluginManifest.builder("notice")
                .pluginName("公告插件")
                .pluginVersion("1.0.0")
                .basePath("/plugins/notice")
                .entryJs("/plugin-static/notice/remoteEntry.js")
                .build();
    }

    @Override
    public void onInstall(PluginContext context) {
    }

    @Override
    public void onStart(PluginContext context) {
    }

    @Override
    public void onStop(PluginContext context) {
    }

    @Override
    public void onUninstall(PluginContext context) {
    }
}
```

---

## 10.5 插件接口访问模式

```java
public enum PluginAccessMode {

    CENTER_ADMIN_ONLY,

    CLIENT_SDK_GATEWAY,

    INTERNAL_ONLY,

    CLIENT_SDK_DIRECT
}
```

说明：

```text
CENTER_ADMIN_ONLY：
默认模式，只允许后台管理端通过 Pyin 管理端网关访问。

CLIENT_SDK_GATEWAY：
允许 C端 SDK 通过 Pyin C端网关访问。

INTERNAL_ONLY：
插件内部接口，不对外暴露。

CLIENT_SDK_DIRECT：
预留能力，P1 默认不开放。
```

默认值：

```text
CENTER_ADMIN_ONLY
```

---

## 10.6 插件后端接口

管理端接口：

```java
@PluginAdminController
@RequestMapping("/admin/notice")
public class NoticeAdminController {

    @GetMapping("/list")
    @AdminApi(permission = @Permission(code = "notice:view", name = "公告查看"))
public Result<?> adminList() {
    return Result.ok();
}
}
```

C端 SDK 接口：

```java
@PluginClientController
@OpenMapping("/notice")
public class NoticeClientController {

    @GetMapping("/list")
    @ClientSdkApi
public Result<?> clientList() {
    return Result.ok();
}
}
```

内部接口：

```java
@RestController
@RequestMapping("/internal/notice")
public class NoticeInternalController {

    @PostMapping("/rebuild-cache")
    @InternalApi
public Result<?> rebuildCache() {
    return Result.ok();
}
}
```

---

## 10.7 插件安全

插件默认只信任 Pyin 签名请求。

Pyin 转发到插件的请求必须带：

```text
X-Pyin-Center-Id
X-Pyin-Plugin-Id
X-Pyin-Request-Source
X-Pyin-Request-Id
X-Pyin-Timestamp
X-Pyin-Nonce
X-Pyin-Body-Sha256
X-Pyin-Signature
```

请求来源：

```text
ADMIN_GATEWAY
CLIENT_SDK_GATEWAY
```

插件 SDK 必须校验：

```text
pluginId 是否匹配
timestamp 是否有效
nonce 是否重复
bodyHash 是否一致
signature 是否正确
当前接口 accessMode 是否允许当前 requestSource
```

插件后端不接受外部直接访问。

外部请求没有 Pyin 签名，直接拒绝。

---

## 10.8 PluginManifest 示例

```java
@PluginComponent
public class NoticePlugin implements PyinPlugin {

    @Override
    public PluginManifest manifest() {
        return PluginManifest.builder("notice")
                .pluginName("公告插件")
                .pluginVersion("1.0.0")
                .basePath("/plugins/notice")
                .entryJs("/plugin-static/notice/remoteEntry.js")
                .build();
    }
}
```

---

# 11. 默认插件客户端接口

## 11.1 配置插件 C端接口

配置插件必须提供：

```text
GET /config/value
GET /config/namespace
GET /config/version
```

经过 Pyin 网关后的完整路径：

```text
GET /plugins/config/open/config/value
GET /plugins/config/open/config/namespace
GET /plugins/config/open/config/version
```

配置变更统一走 Pyin 通知通道：

```text
GET /open/events/stream
```

配置变更流程：

```text
config-plugin
  ↓ 发布 config.changed 事件
pyin-notify
  ↓ 推送给 C端核心 SDK
pyin-client-core-sdk
  ↓ Java 类接口分发给 pyin-client-config-sdk
pyin-client-config-sdk
  ↓ 更新缓存、触发监听器
```

---

## 11.2 字典插件 C端接口

字典插件必须提供：

```text
GET /dict/label
GET /dict/items
GET /dict/batch
```

经过 Pyin 网关后的完整路径：

```text
GET /plugins/dict/open/dict/label
GET /plugins/dict/open/dict/items
GET /plugins/dict/open/dict/batch
```

字典变更事件：

```json
{
  "eventId": "evt_002",
  "featureCode": "dict",
  "eventType": "dict.changed",
  "timestamp": 1710000000000,
  "payload": {
    "typeCode": "gender"
  }
}
```

C端核心 SDK 收到后，本地调用：

```java
DictClientFeature.handleEvent(event);
```

---

# 12. 插件前端规范

插件前端使用模块联邦。

`vite.config.ts` 示例：

```ts
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import federation from '@originjs/vite-plugin-federation'

export default defineConfig({
  plugins: [
    vue(),
    federation({
      name: 'notice',
      filename: 'remoteEntry.js',
      exposes: {
        './NoticeRemoteApp': './src/exposed/NoticeRemoteApp.vue',
        './routes': './src/exposed/routes.ts',
        './NoticePicker': './src/components/NoticePicker.vue'
      },
      shared: ['vue', 'vue-router', 'pinia']
    })
  ],
  build: {
    target: 'esnext'
  }
})
```

插件前端构建输出：

```text
web/
├── remoteEntry.js
└── assets/
```

---

# 13. 插件打包规范

插件构建流程：

```text
1. 编译 backend
2. 编译 frontend
3. 将 frontend/dist 复制到 plugin-package/web
4. 将 backend jar 复制到 plugin-package
5. 通过 backend 中的 PyinPlugin.manifest() 提供插件元数据
6. 打包为 pluginId-plugin.zip
```

最终产物：

```text
notice-plugin.zip
├── plugin-backend.jar
└── web/
    ├── remoteEntry.js
    └── assets/
```

---

# 14. 运行目录

```text
pyin-distribution-parent/runtime/pyin-config-center-runtime/
├── app/
│   └── pyin-config-center.jar
│
├── config/
│   └── application-prod.yml
│
├── logs/
│
├── data/
│   ├── plugin-data/
│   └── upload/
│
├── plugins/
│   ├── system/
│   │   ├── dict/
│   │   └── config/
│   │
│   └── external/
│       ├── notice/
│       └── workflow/
│
└── temp/
```

---

# 15. 核心接口

## 15.1 后台基础鉴权接口

```text
POST   /api/auth/login
POST   /api/auth/logout
GET    /api/auth/current-user
```

`routes.ts` 是壳应用动态加载的固定入口，最小实现如下；点击 `notice` 插件时壳应用会注册并跳转到
`/plugins/notice`。额外暴露模块不需要写入该路由文件，也不会被壳应用自动加载。

```ts
import NoticeRemoteApp from './NoticeRemoteApp.vue'

export default [
  {
    path: '/plugins/notice',
    name: 'plugin-notice',
    component: NoticeRemoteApp
  }
]
```

---

## 15.2 系统插件管理接口

系统管理能力由运行时 ID 为 `system` 的系统插件承载（Maven 模块为 `pyin-plugin-system`），管理端接口统一经过插件网关：

```text
GET    /plugins/system/admin/users
POST   /plugins/system/admin/users
PUT    /plugins/system/admin/users/{id}
DELETE /plugins/system/admin/users/{id}

GET    /plugins/system/admin/roles
POST   /plugins/system/admin/roles
PUT    /plugins/system/admin/roles/{id}
DELETE /plugins/system/admin/roles/{id}

GET    /plugins/system/admin/permissions
POST   /plugins/system/admin/roles/{id}/permissions

GET    /plugins/system/admin/client-credentials
POST   /plugins/system/admin/client-credentials
POST   /plugins/system/admin/client-credentials/{id}/enable
POST   /plugins/system/admin/client-credentials/{id}/disable
POST   /plugins/system/admin/client-credentials/{id}/rotate-secret
GET    /plugins/system/admin/client-credentials/{id}/request-logs
```

---

## 15.3 插件管理接口

```text
GET    /plugins/system/admin/plugins
GET    /plugins/system/admin/plugins/{pluginId}
POST   /plugins/system/admin/plugins/upload
POST   /plugins/system/admin/plugins/{pluginId}/install
POST   /plugins/system/admin/plugins/{pluginId}/stop
POST   /plugins/system/admin/plugins/{pluginId}/restart
POST   /plugins/system/admin/plugins/{pluginId}/upgrade
DELETE /plugins/system/admin/plugins/{pluginId}
GET    /plugins/system/admin/plugins/{pluginId}/logs
GET    /plugins/system/admin/plugins/manifest
GET    /plugins/system/admin/plugins/navigation
```

---

## 15.4 插件管理端转发接口

```text
ANY /plugins/{pluginId}/admin/**
```

---

## 15.5 C端 SDK 认证接口

```text
POST /open/auth/token
```

Token 请求 Header：

```text
X-Pyin-Access-Key
X-Pyin-Timestamp
X-Pyin-Nonce
X-Pyin-Body-Sha256
X-Pyin-Signature
```

---

## 15.6 C端 SDK 通知接口

```text
GET /open/events/stream
```

---

## 15.7 C端 SDK 插件调用接口

```text
ANY /plugins/{pluginId}/open/**
```

---

# 16. 数据库表建议

## 16.1 核心平台表

```text
pyin_user
pyin_role
pyin_permission
pyin_user_role
pyin_role_permission
pyin_token
pyin_login_log
pyin_audit_log

pyin_plugin
pyin_plugin_version
pyin_plugin_runtime_state
pyin_plugin_permission
pyin_plugin_api
pyin_plugin_event_log
pyin_plugin_install_log

pyin_client_credential
pyin_client_token
pyin_client_request_log

pyin_notify_message
pyin_notify_delivery_log
pyin_system_setting
```

---

## 16.2 默认插件表

字典插件表：

```text
plugin_dict_type
plugin_dict_item
plugin_dict_change_log
```

配置插件表：

```text
plugin_config_namespace
plugin_config_item
plugin_config_history
plugin_config_publish_record
plugin_config_notify_record
```

---

# 17. AI 编码规则

AI Agent 修改代码时必须遵守以下规则：

```text
1. 不要把字典、配置业务写入 pyin-core。
2. 字典和配置必须作为系统插件实现。
3. 不要新增 appId/appSecret 模型。
4. C端只使用 accessKey/accessSecret 接入凭证。
5. P1 不做 C端用户登录。
6. P1 不做 namespace/plugin/scope 授权。
7. C端核心 SDK 收到事件后，通过 Java 类接口调用功能 SDK。
8. 核心 SDK 不通过 HTTP 调用功能 SDK。
9. 功能 SDK 可以通过核心 SDK HttpClient 请求 Pyin。
10. 插件后端默认只接受 Pyin 签名请求。
11. 插件管理端接口默认 CENTER_ADMIN_ONLY。
12. 插件 C端接口必须显式声明 CLIENT_SDK_GATEWAY。
13. 插件前端必须使用模块联邦。
14. 功能 SDK starter 名称必须见名知意。
15. 功能 SDK starter 统一使用 pyin-client-xxx-spring-starter。
16. 禁止使用过短命名，例如 pyin-config-starter、pyin-dict-starter。
17. 禁止使用过长命名，例如 pyin-client-config-spring-boot-starter。
18. 服务端插件运行时模块使用 pyin-plugin-runtime。
19. 服务端插件网关模块使用 pyin-plugin-gateway。
20. 前端壳模块使用 pyin-web-shell。
21. 所有核心自动装配 Bean 必须使用 @ConditionalOnMissingBean。
22. 多个功能 SDK 同时引入时，只允许初始化一个核心 SDK。
23. 多个功能 SDK 同时引入时，只允许建立一个通知通道。
24. 插件最终必须打包为 zip，包含 backend jar、web/remoteEntry.js 与 web/assets/；插件元数据由 Java `PluginManifest` 提供。
25. 系统插件和外部插件必须走同一套加载机制。
26. 不要把核心 SDK 打入功能 SDK fat jar，只能使用 Maven 依赖传递。
27. 功能 SDK 不允许直接读取 accessSecret。
28. 认证、Token、HTTP Client、通知连接必须由 pyin-client-core-sdk 统一管理。
```

---

# 18. P1 最终定位总结

Pyin P1 是一个插件化配置中心平台。

```text
Pyin 配置中心核心负责：
用户管理、权限管理、后台鉴权、插件运行时、插件请求转发、插件状态监听、通知中心、接入凭证管理。

字典管理和配置管理都是默认系统插件。

C端业务系统通过 Pyin C端 SDK 接入配置中心。
C端不使用 appId，不需要用户登录。
C端只配置配置中心地址和接入凭证。

用户想使用哪个 C端能力，就引入哪个功能 SDK。

核心 SDK 收到配置中心通知后，通过 Java 类接口分发给功能 SDK。

功能 SDK 自己处理缓存刷新、业务回调和必要的数据拉取。

插件默认只接受 Pyin 中心签名请求。

插件前端通过模块联邦动态加载。

插件后端通过 Pyin 网关统一转发。
```
