import { createApp } from 'vue'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import ConfigRemoteApp from './exposed/ConfigRemoteApp.vue'

createApp(ConfigRemoteApp).use(ElementPlus).mount('#app')
