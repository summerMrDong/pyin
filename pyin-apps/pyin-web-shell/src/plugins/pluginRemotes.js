import {
  __federation_method_getRemote as getRemote,
  __federation_method_setRemote as setRemote,
  __federation_method_unwrapDefault as unwrapDefault
} from '__federation__'

const remoteRegistrationState = new Map()

function buildRemoteConfig(remoteEntry) {
  return {
    url: remoteEntry,
    format: 'esm',
    from: 'vite'
  }
}

async function ensureRemoteRegistered(remoteName, remoteEntry) {
  if (!remoteName || !remoteEntry) {
    return false
  }

  const existing = remoteRegistrationState.get(remoteName)
  if (existing === 'ready') {
    return true
  }
  if (existing instanceof Promise) {
    return existing
  }

  const registration = Promise.resolve().then(() => {
    setRemote(remoteName, buildRemoteConfig(remoteEntry))
    remoteRegistrationState.set(remoteName, 'ready')
    return true
  }).catch((error) => {
    remoteRegistrationState.delete(remoteName)
    throw error
  })

  remoteRegistrationState.set(remoteName, registration)
  return registration
}

async function loadRemoteModule(remoteName, exposedModule) {
  const remoteModule = await getRemote(remoteName, exposedModule)
  return unwrapDefault(remoteModule)
}

function buildComponentRoute(plugin, component) {
  if (!component) {
    return []
  }
  const menus = flattenRouteMenus(plugin.menus ?? [])
  if (menus.length === 0) {
    return []
  }
  return menus.map((menu) => ({
    path: menu.path,
    component,
    props: { page: menu.page ?? null }
  }))
}

function flattenRouteMenus(menus) {
  return menus.flatMap((menu) => {
    if (menu.type === 'DIRECTORY') {
      return flattenRouteMenus(menu.children ?? [])
    }
    if (menu.type === 'ROUTE' && menu.path) {
      return [menu]
    }
    return []
  })
}

export async function loadRemoteRoutes(plugin) {
  const frontend = plugin?.frontend
  if (!frontend) {
    return []
  }

  const remoteName = frontend.remoteName
  const remoteEntry = frontend.remoteEntry
  const exposedModules = frontend.exposedModules ?? []

  if (!remoteName || !remoteEntry) {
    return []
  }

  await ensureRemoteRegistered(remoteName, remoteEntry)

  try {
    const routesModule = await loadRemoteModule(remoteName, './routes')
    const routes = Array.isArray(routesModule) ? routesModule
      : Array.isArray(routesModule?.default) ? routesModule.default
      : []
    if (routes.length > 0) {
      return routes
    }
  } catch (error) {
    console.warn(`[pyin-web-shell] failed to load remote routes for ${plugin.pluginId}`, error)
  }

  for (const mod of exposedModules) {
    try {
      const component = await loadRemoteModule(remoteName, mod)
      const routes = buildComponentRoute(plugin, component)
      if (routes.length > 0) {
        return routes
      }
    } catch (error) {
      console.warn(`[pyin-web-shell] failed to load remote module ${mod} for ${plugin.pluginId}`, error)
    }
  }

  return []
}
