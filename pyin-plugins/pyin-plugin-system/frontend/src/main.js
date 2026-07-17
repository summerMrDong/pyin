import { createApp } from 'vue'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import PyinSystemRemoteApp from './exposed/PyinSystemRemoteApp.vue'

createApp(PyinSystemRemoteApp).use(ElementPlus).mount('#app')
