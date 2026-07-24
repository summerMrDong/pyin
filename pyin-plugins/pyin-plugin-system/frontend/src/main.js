import { createApp, h } from 'vue'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import { createRouter, createWebHistory, RouterView } from 'vue-router'
import routes from './exposed/routes'

const router = createRouter({
  history: createWebHistory(),
  routes
})

createApp({ render: () => h(RouterView) }).use(router).use(ElementPlus).mount('#app')
