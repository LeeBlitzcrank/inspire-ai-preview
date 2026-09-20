import { createApp } from 'vue'
import App from './App.vue'
import router from './router'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import { pinia } from './stores'
import { useAuthStore } from './stores/auth'

const app = createApp(App)
app.use(pinia)
// 应用启动时同步初始化登录态，页面/路由守卫可立即读取
useAuthStore().init()
app.use(router)
app.use(ElementPlus)
app.mount('#app')
