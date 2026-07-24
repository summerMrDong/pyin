import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ExportWorkshopRemoteApp from './exposed/ExportWorkshopRemoteApp.vue'

createApp(ExportWorkshopRemoteApp).use(createPinia()).mount('#app')
