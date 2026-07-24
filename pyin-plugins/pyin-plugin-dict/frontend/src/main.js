import { createApp } from 'vue'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import DictRemoteApp from './exposed/DictRemoteApp.vue'

createApp(DictRemoteApp).use(ElementPlus).mount('#app')
