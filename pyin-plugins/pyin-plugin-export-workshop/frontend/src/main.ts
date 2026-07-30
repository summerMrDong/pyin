import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import ExportWorkshopRemoteApp from './exposed/ExportWorkshopRemoteApp.vue'

createApp(ExportWorkshopRemoteApp).use(createPinia()).use(ElementPlus).mount('#app')
