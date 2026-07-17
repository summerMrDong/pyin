import DictRemoteApp from './DictRemoteApp.vue'

export const routeDefinitions = [
  {
    code: 'dict',
    path: '/plugins/dict',
    component: 'DictRemoteApp',
    title: '字典管理',
    icon: 'BookKey',
    sort: 110,
    requireLogin: true,
    permissionCode: 'dict:view'
  }
]

const componentRegistry = {
  DictRemoteApp
}

export default routeDefinitions.map((route) => ({
  path: route.path,
  component: componentRegistry[route.component]
}))
