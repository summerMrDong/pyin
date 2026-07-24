import {
  __federation_method_getRemote as getRemote,
  __federation_method_setRemote as setRemote,
  __federation_method_unwrapDefault as unwrapDefault
} from '__federation__'

const remoteRegistrationState = new Map()

function buildRemoteConfig(remoteEntry) {
  return { url: remoteEntry, format: 'esm', from: 'vite' }
}

async function ensureRemoteRegistered(remoteName, remoteEntry) {
  if (!remoteName || !remoteEntry) return false

  const existing = remoteRegistrationState.get(remoteName)
  if (existing === 'ready') return true
  if (existing instanceof Promise) return existing

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

/**
 * 加载插件自行声明的路由。`./routes` 是所有插件必须暴露的联邦入口，Shell 不推导或补充页面路由。
 */
export async function loadRemoteRoutes(plugin) {
  const frontend = plugin?.frontend
  if (!plugin?.pluginId || !frontend?.remoteEntry) {
    return { routes: [], failed: false }
  }

  try {
    await ensureRemoteRegistered(plugin.pluginId, frontend.remoteEntry)
    const remoteModule = await getRemote(plugin.pluginId, './routes')
    const routesModule = unwrapDefault(remoteModule)
    const routes = Array.isArray(routesModule)
      ? routesModule
      : Array.isArray(routesModule?.default) ? routesModule.default : []
    return { routes, failed: routes.length === 0 }
  } catch (error) {
    console.warn(`[pyin-web-shell] failed to load routes for ${plugin.pluginId}`, error)
    return { routes: [], failed: true }
  }
}
