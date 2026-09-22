import { createApp } from 'vue'
import App from './App.vue'
import router from './router'
// Element Plus 改为按需引入（见 vite.config.js 的 unplugin-vue-components）。
// 模板里的 el-xxx 组件会自动按需加载，这里只手动补上「命令式调用」用到的样式。
import 'element-plus/es/components/message/style/css'
import 'element-plus/es/components/message-box/style/css'
import 'element-plus/es/components/loading/style/css'
import { pinia } from './stores'
import { useAuthStore } from './stores/auth'

const app = createApp(App)
app.use(pinia)
// 应用启动时同步初始化登录态，页面/路由守卫可立即读取
useAuthStore().init()
app.use(router)
app.mount('#app')
