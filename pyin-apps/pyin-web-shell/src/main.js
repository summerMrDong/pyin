import { createApp } from 'vue'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import { createPinia } from 'pinia'
import App from './App.vue'
import router from './router'
import { useAuthStore } from './stores/auth'
import { useShellNavigationStore } from './stores/shellNavigation'
import { useThemeStore } from './stores/theme'
import './styles/theme.css'

const pinia = createPinia()
const authStore = useAuthStore(pinia)
const themeStore = useThemeStore(pinia)
const navigationStore = useShellNavigationStore(pinia)

themeStore.initialize()
await authStore.initialize()
await navigationStore.initialize(router)

router.beforeEach(async (to) => {
  if (authStore.initializing) {
    return true
  }

  if (to.meta.public) {
    if (to.path === '/login' && authStore.isAuthenticated) {
      return '/'
    }
    return true
  }

  if (!authStore.isAuthenticated) {
    return {
      path: '/login',
      query: to.fullPath && to.fullPath !== '/' ? { redirect: to.fullPath } : undefined
    }
  }

  return true
})

window.addEventListener('pyin-auth-expired', async () => {
  authStore.clearAuth()
  if (router.currentRoute.value.path !== '/login') {
    await router.replace({
      path: '/login',
      query: { redirect: router.currentRoute.value.fullPath }
    })
  }
})

createApp(App).use(pinia).use(router).use(ElementPlus).mount('#app')
