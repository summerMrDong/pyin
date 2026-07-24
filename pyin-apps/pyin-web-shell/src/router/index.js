import { createRouter, createWebHistory } from 'vue-router'
import LinkViewer from '../views/LinkViewer.vue'
import LoginView from '../views/LoginView.vue'

export const coreRoutes = [
  { path: '/login', component: LoginView, meta: { public: true, layout: 'auth' } },
  { path: '/__link-viewer', component: LinkViewer, meta: { moduleCode: 'system', menuCode: '__link-viewer' } }
]

export default createRouter({
  history: createWebHistory(),
  routes: coreRoutes
})
