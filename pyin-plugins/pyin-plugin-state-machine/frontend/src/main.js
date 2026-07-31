import { createApp } from 'vue'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import StateMachineRemoteApp from './exposed/StateMachineRemoteApp.vue'

createApp(StateMachineRemoteApp).use(ElementPlus).mount('#app')
