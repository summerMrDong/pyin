import { defineStore } from 'pinia'
import { httpRequest } from '../api/http'
import PlaceholderView from '../views/PlaceholderView.vue'
import { loadRemoteRoutes } from '../plugins/pluginRemotes'

const SYSTEM_PLUGIN_ID = 'system'
const loadedPluginRouteIds = new Set()
const loadingPluginRoutes = new Map()

function pluginBasePath(pluginId) {
  return `/plugins/${pluginId}`
}

function pluginIdFromPath(path) {
  const match = /^\/plugins\/([^/]+)(?:\/|$)/.exec(path)
  return match?.[1] ?? null
}

function buildModules(plugins) {
  return plugins
    .filter((plugin) => plugin.status === 'STARTED')
    .map((plugin) => ({
      code: `plugin:${plugin.pluginId}`,
      pluginId: plugin.pluginId,
      name: plugin.pluginName,
      icon: plugin.pluginId === SYSTEM_PLUGIN_ID ? 'LayoutGrid' : 'Puzzle',
      defaultPath: pluginBasePath(plugin.pluginId)
    }))
}

function createUnavailableRoute(plugin, moduleCode) {
  return {
    path: `${pluginBasePath(plugin.pluginId)}/:pathMatch(.*)*`,
    component: PlaceholderView,
    props: {
      title: `${plugin.pluginName} 暂不可用`,
      description: '插件未提供有效的 ./routes 路由入口，或其前端资源加载失败。'
    },
    meta: { moduleCode, pluginId: plugin.pluginId }
  }
}

export const useShellNavigationStore = defineStore('shell-navigation', {
  state: () => ({
    plugins: [],
    modules: [],
    loadingPluginIds: {},
    currentModuleCode: null,
    initialized: false
  }),
  getters: {
    currentModule(state) {
      return state.modules.find((module) => module.code === state.currentModuleCode) ?? state.modules[0] ?? null
    }
  },
  actions: {
    async initialize(router) {
      loadedPluginRouteIds.clear()
      loadingPluginRoutes.clear()
      await this.refresh(router)
      this.initialized = true
    },
    async refresh(router) {
      this.plugins = await this.fetchPluginWorkspaces()
      this.modules = buildModules(this.plugins)
      this.syncCurrentModule(router.currentRoute.value)
    },
    async fetchPluginWorkspaces() {
      try {
        const result = await httpRequest('/plugins/system/admin/plugins/workspaces')
        return Array.isArray(result.data) ? result.data : []
      } catch (error) {
        console.warn('[pyin-web-shell] failed to fetch plugin workspaces', error)
        return []
      }
    },
    async ensurePluginRoutes(pluginId, router) {
      if (loadedPluginRouteIds.has(pluginId)) return true

      const plugin = this.plugins.find((item) => item.pluginId === pluginId && item.status === 'STARTED')
      if (!plugin) return false

      const pending = loadingPluginRoutes.get(pluginId)
      if (pending) return pending

      this.setPluginLoading(pluginId, true)
      const loading = this.registerPluginRoutes(router, plugin)
        .then((loaded) => {
          if (loaded) loadedPluginRouteIds.add(pluginId)
          return loaded
        })
        .finally(() => {
          loadingPluginRoutes.delete(pluginId)
          this.setPluginLoading(pluginId, false)
        })

      loadingPluginRoutes.set(pluginId, loading)
      return loading
    },
    setPluginLoading(pluginId, loading) {
      const next = { ...this.loadingPluginIds }
      if (loading) next[pluginId] = true
      else delete next[pluginId]
      this.loadingPluginIds = next
    },
    async ensureRouteForPath(path, router) {
      const pluginId = pluginIdFromPath(path)
      return pluginId ? this.ensurePluginRoutes(pluginId, router) : false
    },
    async registerPluginRoutes(router, plugin) {
      const moduleCode = `plugin:${plugin.pluginId}`
      const { routes, failed } = await loadRemoteRoutes(plugin)
      const entryPath = pluginBasePath(plugin.pluginId)
      const hasEntryRoute = routes.some((route) => route.path === entryPath)

      if (!failed && hasEntryRoute) {
        for (const route of routes) {
          router.addRoute({
            ...route,
            meta: { ...(route.meta ?? {}), moduleCode, pluginId: plugin.pluginId }
          })
        }
        return true
      }

      router.addRoute(createUnavailableRoute(plugin, moduleCode))
      return true
    },
    syncCurrentModule(routeOrPath) {
      const route = typeof routeOrPath === 'string' ? { path: routeOrPath, meta: {} } : routeOrPath
      const moduleCode = route?.meta?.moduleCode
      const pluginId = route?.meta?.pluginId ?? pluginIdFromPath(route?.path ?? '')
      this.currentModuleCode = moduleCode
        ?? (pluginId ? `plugin:${pluginId}` : this.modules[0]?.code ?? null)
    },
    async activateModule(moduleCode, router) {
      const module = this.modules.find((item) => item.code === moduleCode)
      if (!module) return

      this.currentModuleCode = moduleCode
      const loaded = await this.ensurePluginRoutes(module.pluginId, router)
      if (loaded && router.currentRoute.value.meta?.pluginId !== module.pluginId) {
        await router.push(module.defaultPath)
      }
    }
  }
})
