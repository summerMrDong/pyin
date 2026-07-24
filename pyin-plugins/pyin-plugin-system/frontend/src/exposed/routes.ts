import PyinSystemRemoteApp from './PyinSystemRemoteApp.vue'

const pageRoutes = [
  ['dashboard', ''],
  ['users', 'users'],
  ['roles', 'roles'],
  ['permissions', 'permissions'],
  ['plugins', 'plugins'],
  ['credentials', 'credentials'],
  ['settings', 'settings']
]

export default pageRoutes.map(([page, suffix]) => ({
  path: suffix ? `/plugins/system/${suffix}` : '/plugins/system',
  name: `plugin-system-${page}`,
  component: PyinSystemRemoteApp,
  props: { page }
}))
