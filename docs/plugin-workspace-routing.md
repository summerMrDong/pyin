# 插件工作区与前端路由规范

本文是 Pyin 当前插件前端集成的唯一规则。开发插件、审查插件或回答插件开发问题时，以本文和仓库根目录 `AGENTS.md` 为准。

## 1. 模型与职责

Pyin 使用“插件工作区”模型：顶部栏展示已启动插件，点击插件 `pluginId` 后主前端壳固定跳转到
`/plugins/{pluginId}`。壳应用只负责发现插件、加载 `remoteEntry.js` 与动态注册插件路由；下方页面、
二级导航、页签和局部状态全部由插件前端负责。

平台没有插件菜单契约。不得实现或恢复 `PyinPlugin.menus()`、`PluginMenu`、`menus.ts`、菜单树接口，
也不得让壳应用根据后端菜单推导插件页面。

后端接口权限仍是安全边界。前端路由、按钮隐藏和工作区标签都不能替代 `@AdminApi` 与权限校验。

## 2. 必须遵守的前端契约

每个插件至少包含：

```text
frontend/src/exposed/
├── <PluginRemoteApp>.vue
└── routes.ts
```

模块联邦必须暴露 `./routes`。它的默认导出必须是 Vue Router 路由记录数组，并且必须存在一条与插件 ID
完全匹配的入口路由 `/plugins/{pluginId}`。Shell 会先加载、注册该数组，再跳转到该入口路径。

```ts
import NoticeRemoteApp from './NoticeRemoteApp.vue'

export default [
  {
    path: '/plugins/notice',
    name: 'plugin-notice',
    component: NoticeRemoteApp
  },
  {
    path: '/plugins/notice/history',
    name: 'plugin-notice-history',
    component: () => import('../views/NoticeHistoryView.vue')
  }
]
```

路由路径必须使用 `/plugins/{pluginId}` 前缀。若插件需要默认子页面，可在入口路由上使用 Vue Router 的
`redirect`，或由入口组件自行决定初始内容；不得要求壳应用传入 `page`、菜单编码或默认路由参数。

前端必须将 `vue`、`vue-router`、`pinia` 作为模块联邦共享依赖。插件应使用主前端壳共享的 Router，不能
额外创建浏览器 History 实例；这样刷新、浏览器前进后退和直接访问内部 URL 才会正常工作。

## 3. 多模块暴露

`./routes` 只是壳应用保留的路由入口，不限制插件暴露其他模块。插件可继续通过 Vite federation 的
`exposes` 发布可复用组件、组合式函数和前端能力：

```ts
exposes: {
  './NoticeRemoteApp': './src/exposed/NoticeRemoteApp.vue',
  './routes': './src/exposed/routes.ts',
  './NoticePicker': './src/components/NoticePicker.vue',
  './useNotice': './src/composables/useNotice.ts'
}
```

其他插件若要引用这些能力，应显式使用提供方的 `pluginId`（也是 federation `name`）与模块名，例如
`notice` 的 `./NoticePicker`。不得假定壳应用会自动加载、注册或注入这些额外模块。跨插件共享能力稳定后，优先
评估是否需要新增该插件的 Java `api` 模块；不要把后端业务实现暴露为公共 API。

## 4. 后端清单与工作区发现

插件入口仍只通过 `PyinPlugin.manifest()` 提供元数据。`entryJs` 用于加载远程模块；模块联邦的
`name` 必须等于 `pluginId`，Shell 据此加载固定的 `./routes`。额外暴露模块不登记在 Manifest，
而由提供方和消费方按模块名约定。

主前端壳从 `GET /plugins/system/admin/plugins/workspaces` 获取已启动插件的 ID、名称和模块联邦
加载信息。该接口不返回页面菜单、默认页面、路由树或页面组件。插件进入工作区后，全部页面选择由其
`routes.ts` 决定。

插件 ID 必须且只能在 `PluginManifest` 中声明：

```java
return PluginManifest.builder("notice")
        .pluginName("公告插件")
        .build();
```

不要在 `PyinPlugin` 上重复声明 `pluginId()`，也不要使用其他配置文件定义第二个插件 ID 来源。

插件运行来源也不在 Manifest 中声明。中心 classpath 内且位于平台 `embedded-plugin-ids` 白名单的系统
插件会被注册为 `EMBEDDED_SYSTEM`；其他插件必须作为独立进程通过节点注册协议接入，并被注册为
`STANDALONE_NODE`。外部插件不得依靠 Manifest 要求内嵌执行。

## 5. 刷新、深链接与登录回跳

访问 `/plugins/{pluginId}` 或 `/plugins/{pluginId}/**` 时，浏览器必须先获得主前端壳的 `index.html`。
随后 Shell 根据当前 URL 提取 `pluginId`，加载该插件的 `./routes` 并重新匹配原地址。

部署主前端壳的 Web 服务器必须只对 HTML 页面请求执行 SPA 回退：

```text
GET /plugins/{pluginId}/** + Accept: text/html  ->  /index.html
```

不得将真实接口或静态资源回退到 HTML，至少排除：

```text
/plugins/{pluginId}/admin/**
/plugins/{pluginId}/open/**
/plugins/system/admin/**
/plugin-static/**
```

开发环境同样不得把全部 `/plugins/**` 代理到后端；只代理 `admin` 与 `open` API。否则刷新插件页面会绕过
Vite 的 SPA fallback 并得到后端 404。

未登录用户访问插件深链接时，Shell 必须跳到 `/login?redirect=<原始完整路径>`。登录成功后先加载工作区
列表，再回到该 `redirect`；路由守卫会按上述规则延迟注册插件路由。

## 6. 开发与验收清单

开发或修改插件前端时必须确认：

- `routes.ts` 已在 federation `exposes` 中以 `./routes` 暴露。
- 默认导出是路由数组，并包含精确的 `/plugins/{pluginId}` 入口。
- 所有子页面使用 `/plugins/{pluginId}/**`，不占用其他插件或核心页面路径。
- 直接访问子页面 URL、刷新页面、浏览器前进和后退都可由 Shell 延迟加载该插件路由后正常显示。
- 插件可任选内部导航布局，但不向后端或 Shell 维护重复菜单定义。
- 管理端 API 使用 `@PluginAdminController` 与 `@AdminApi`，并通过后端权限验证；前端展示不作为授权依据。

若 `./routes` 缺失、默认导出不是路由数组，或未声明 `/plugins/{pluginId}`，Shell 会显示该插件工作区不可用。
