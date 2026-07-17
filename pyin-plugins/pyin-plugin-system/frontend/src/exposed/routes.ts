import { h } from 'vue'
import CredentialsView from '../views/CredentialsView.vue'
import DashboardView from '../views/DashboardView.vue'
import PlaceholderView from '../views/PlaceholderView.vue'
import RolesView from '../views/RolesView.vue'
import UsersView from '../views/UsersView.vue'

export const routeDefinitions = [
  {
    code: 'dashboard',
    path: '/',
    component: 'DashboardView',
    title: '控制台',
    icon: 'LayoutDashboard',
    sort: 0,
    requireLogin: true,
    permissionCode: 'system:view'
  },
  {
    code: 'users',
    path: '/users',
    component: 'UsersView',
    title: '用户管理',
    icon: 'Users',
    sort: 10,
    requireLogin: true,
    permissionCode: 'user:view'
  },
  {
    code: 'roles',
    path: '/roles',
    component: 'RolesView',
    title: '角色管理',
    icon: 'ShieldCheck',
    sort: 20,
    requireLogin: true,
    permissionCode: 'role:view'
  },
  {
    code: 'permissions',
    path: '/permissions',
    component: 'PermissionsPlaceholderView',
    title: '权限管理',
    icon: 'KeyRound',
    sort: 30,
    requireLogin: true,
    permissionCode: 'system:view'
  },
  {
    code: 'plugins',
    path: '/plugins',
    component: 'PluginsPlaceholderView',
    title: '插件管理',
    icon: 'Blocks',
    sort: 40,
    requireLogin: true,
    permissionCode: 'system:view'
  },
  {
    code: 'credentials',
    path: '/credentials',
    component: 'CredentialsView',
    title: '接入凭证',
    icon: 'BadgeCheck',
    sort: 50,
    requireLogin: true,
    permissionCode: 'system:view'
  },
  {
    code: 'settings',
    path: '/settings',
    component: 'SettingsPlaceholderView',
    title: '系统设置',
    icon: 'Settings2',
    sort: 60,
    requireLogin: true,
    permissionCode: 'system:view'
  }
]

const componentRegistry = {
  DashboardView,
  UsersView,
  RolesView,
  CredentialsView,
  PermissionsPlaceholderView: {
    render() {
      return h(PlaceholderView, {
        title: '权限管理',
        description: '系统权限配置页已迁入 pyin-system 插件，后续可继续接入更细粒度功能。'
      })
    }
  },
  PluginsPlaceholderView: {
    render() {
      return h(PlaceholderView, {
        title: '插件管理',
        description: '系统插件管理入口已纳入 pyin-system 插件页面。'
      })
    }
  },
  SettingsPlaceholderView: {
    render() {
      return h(PlaceholderView, {
        title: '系统设置',
        description: '系统设置页已迁入 pyin-system 插件，后续可继续补充真实配置项。'
      })
    }
  }
}

export default routeDefinitions.map((route) => ({
  path: route.path,
  component: componentRegistry[route.component]
}))
