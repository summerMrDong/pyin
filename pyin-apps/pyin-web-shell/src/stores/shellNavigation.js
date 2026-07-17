import { defineStore } from 'pinia'
import { httpRequest } from '../api/http'
import PlaceholderView from '../views/PlaceholderView.vue'
import { loadRemoteRoutes } from '../plugins/pluginRemotes'

const SYSTEM_MODULE_CODE = 'pyin-system'
const LINK_VIEWER_PATH = '/__link-viewer'
const registeredRoutePaths = new Set()

function normalizeMenu(menu, pluginId = SYSTEM_MODULE_CODE) {
  return {
    code: menu.code,
    name: menu.name,
    type: menu.type ?? 'ROUTE',
    path: menu.path,
    url: menu.url ?? null,
    icon: menu.icon,
    sort: menu.sort ?? 999,
    permissionCode: menu.permissionCode ?? null,
    openMode: menu.openMode ?? null,
    pluginId,
    page: menu.page ?? null,
    children: (menu.children ?? []).map((child) => normalizeMenu(child, pluginId))
  }
}

function sortMenus(left, right) {
  return (left.sort ?? 999) - (right.sort ?? 999) || left.name.localeCompare(right.name, 'zh-CN')
}

function buildSystemModule(plugin) {
  const menus = (plugin?.menus ?? []).map((menu) => normalizeMenu(menu))
  menus.sort(sortMenus)

  return {
    code: SYSTEM_MODULE_CODE,
    name: plugin?.pluginName ?? '系统模块',
    icon: firstMenuIcon(menus) ?? 'LayoutGrid',
    kind: 'SYSTEM',
    defaultPath: findFirstNavigablePath(menus) ?? '/',
    menus
  }
}

function buildPluginModules(plugins) {
  return plugins
    .filter((plugin) => plugin.status === 'STARTED' && plugin.pluginId !== SYSTEM_MODULE_CODE)
    .map((plugin) => {
      const menus = (plugin.menus ?? [])
        .map((menu) => normalizeMenu(menu, plugin.pluginId))
        .sort(sortMenus)

      return {
        code: `plugin:${plugin.pluginId}`,
        name: plugin.pluginName,
        icon: firstMenuIcon(menus) ?? 'Puzzle',
        kind: plugin.pluginType,
        defaultPath: findFirstNavigablePath(menus) ?? '/plugins',
        pluginId: plugin.pluginId,
        menus
      }
    })
    .sort((left, right) => {
      const leftSort = findFirstMenuSort(left.menus)
      const rightSort = findFirstMenuSort(right.menus)
      return leftSort - rightSort || left.name.localeCompare(right.name, 'zh-CN')
    })
}

function buildModules(plugins) {
  const systemPlugin = plugins.find((plugin) => plugin.pluginId === SYSTEM_MODULE_CODE)
  return [buildSystemModule(systemPlugin), ...buildPluginModules(plugins)]
}

function createFallbackRoute(plugin, menu, moduleCode) {
  return {
    path: menu.path,
    component: PlaceholderView,
    props: {
      title: menu.name,
      description: `${plugin.pluginName} 已纳入模块导航，但前端远端暂未接入或未启动。`
    },
    meta: {
      moduleCode,
      menuCode: menu.code,
      pluginId: plugin.pluginId
    }
  }
}

function flattenMenus(menus) {
  return menus.flatMap((menu) => {
    if (menu.type === 'DIRECTORY') {
      return flattenMenus(menu.children ?? [])
    }
    return [menu]
  })
}

function findFirstNavigablePath(menus) {
  for (const menu of flattenMenus(menus)) {
    if (menu.type === 'ROUTE' && menu.path) {
      return menu.path
    }
    if (menu.type === 'LINK' && menu.url) {
      return buildLinkViewerPath(menu)
    }
  }
  return null
}

function findFirstMenuSort(menus) {
  return flattenMenus(menus)[0]?.sort ?? 999
}

function firstMenuIcon(menus) {
  return flattenMenus(menus).find((menu) => menu.icon)?.icon ?? null
}

function buildLinkViewerPath(menu) {
  const params = new URLSearchParams()
  if (menu.url) params.set('url', menu.url)
  if (menu.name) params.set('title', menu.name)
  if (menu.code) params.set('menuCode', menu.code)
  if (menu.pluginId) {
    params.set('moduleCode', menu.pluginId === SYSTEM_MODULE_CODE ? SYSTEM_MODULE_CODE : `plugin:${menu.pluginId}`)
  }
  return `${LINK_VIEWER_PATH}?${params.toString()}`
}

function matchesMenuPath(menu, routePath) {
  if (menu.type === 'DIRECTORY') {
    return (menu.children ?? []).some((child) => matchesMenuPath(child, routePath))
  }
  if (menu.type === 'ROUTE' && menu.path) {
    return routePath === menu.path || routePath.startsWith(`${menu.path}/`)
  }
  if (menu.type === 'LINK' && menu.url) {
    return routePath === LINK_VIEWER_PATH
  }
  return false
}

function resolveMenuTarget(menu) {
  if (menu.type === 'ROUTE') {
    return menu.path
  }
  if (menu.type === 'LINK' && menu.url) {
    return buildLinkViewerPath(menu)
  }
  return null
}

export const useShellNavigationStore = defineStore('shell-navigation', {
  state: () => ({
    modules: [],
    currentModuleCode: SYSTEM_MODULE_CODE,
    initialized: false
  }),
  getters: {
    currentModule(state) {
      return state.modules.find((module) => module.code === state.currentModuleCode) ?? state.modules[0] ?? null
    },
    currentMenus() {
      return this.currentModule?.menus ?? []
    }
  },
  actions: {
    async initialize(router) {
      registeredRoutePaths.clear()
      for (const route of router.getRoutes()) {
        registeredRoutePaths.add(route.path)
      }
      await this.refresh(router)
      this.initialized = true
    },
    async refresh(router) {
      const plugins = await this.fetchPluginNavigation()
      this.modules = buildModules(plugins)
      await this.registerPluginRoutes(router, plugins)
      this.syncCurrentModule(router.currentRoute.value)
    },
    async fetchPluginNavigation() {
      try {
        const result = await httpRequest('/api/core/plugins/navigation')
        return Array.isArray(result.data) ? result.data : []
      } catch (error) {
        console.warn('[pyin-web-shell] failed to fetch plugin navigation', error)
        return []
      }
    },
    async registerPluginRoutes(router, plugins) {
      console.log('[pyin-web-shell] fetch plugin navigation', plugins)
      for (const plugin of plugins) {
        const moduleCode = plugin.pluginId === SYSTEM_MODULE_CODE ? SYSTEM_MODULE_CODE : `plugin:${plugin.pluginId}`
        const remoteRoutes = await loadRemoteRoutes(plugin)

        if (remoteRoutes.length > 0) {
          for (const route of remoteRoutes) {
            if (registeredRoutePaths.has(route.path)) {
              continue
            }
            const matchedMenu = flattenMenus(plugin.menus ?? []).find((menu) => menu.path === route.path)
            router.addRoute({
              ...route,
              meta: {
                ...(route.meta ?? {}),
                moduleCode,
                menuCode: matchedMenu?.code ?? plugin.pluginId,
                pluginId: plugin.pluginId
              }
            })
            registeredRoutePaths.add(route.path)
          }
          continue
        }

        for (const menu of flattenMenus(plugin.menus ?? [])) {
          if (menu.type !== 'ROUTE' || registeredRoutePaths.has(menu.path)) {
            continue
          }
          router.addRoute(createFallbackRoute(plugin, menu, moduleCode))
          registeredRoutePaths.add(menu.path)
        }
      }
    },
    syncCurrentModule(routeOrPath) {
      const route = typeof routeOrPath === 'string'
        ? { path: routeOrPath, meta: {} }
        : (routeOrPath ?? { path: '/', meta: {} })
      const routeQueryModuleCode = route.query?.moduleCode

      if (routeQueryModuleCode) {
        const moduleByQuery = this.modules.find((module) => module.code === routeQueryModuleCode)
        if (moduleByQuery) {
          this.currentModuleCode = moduleByQuery.code
          return
        }
      }
      const routeModuleCode = route.meta?.moduleCode

      if (routeModuleCode) {
        const moduleByMeta = this.modules.find((module) => module.code === routeModuleCode)
        if (moduleByMeta) {
          this.currentModuleCode = moduleByMeta.code
          return
        }
      }

      const matchedModule = [...this.modules]
        .sort((left, right) => (right.defaultPath?.length ?? 0) - (left.defaultPath?.length ?? 0))
        .find((module) =>
          module.menus.some((menu) => matchesMenuPath(menu, route.path))
        )
      this.currentModuleCode = matchedModule?.code ?? SYSTEM_MODULE_CODE
    },
    async activateModule(moduleCode, router) {
      const module = this.modules.find((item) => item.code === moduleCode)
      if (!module) {
        return
      }

      this.currentModuleCode = moduleCode
      const currentPath = router.currentRoute.value.path
      const belongsToModule = module.menus.some((menu) => matchesMenuPath(menu, currentPath))
      if (!belongsToModule && module.defaultPath) {
        await router.push(module.defaultPath)
      }
    },
    resolveMenuTarget(menu) {
      return resolveMenuTarget(menu)
    }
  }
})
