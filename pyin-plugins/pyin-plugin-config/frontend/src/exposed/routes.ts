import ConfigRemoteApp from './ConfigRemoteApp.vue'

export const routeDefinitions = [
  {
    code: 'config',
    path: '/plugins/config',
    component: 'ConfigRemoteApp',
    title: '配置管理',
    icon: 'SlidersHorizontal',
    sort: 120,
    requireLogin: true,
    permissionCode: 'config:view'
  }
]

const componentRegistry = {
  ConfigRemoteApp
}

export default routeDefinitions.map((route) => ({
  path: route.path,
  component: componentRegistry[route.component]
}))
