import FileRemoteApp from './FileRemoteApp.vue'

export const routeDefinitions = [
  {
    code: 'file',
    path: '/plugins/file',
    component: 'FileRemoteApp',
    title: '文件管理',
    icon: 'FolderOpen',
    sort: 160,
    requireLogin: true,
    permissionCode: 'file:view'
  }
]

const componentRegistry = {
  FileRemoteApp
}

export default routeDefinitions.map((route) => ({
  path: route.path,
  component: componentRegistry[route.component]
}))
