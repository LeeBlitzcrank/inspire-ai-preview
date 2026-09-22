import { createApp } from 'vue'
import App from './App.vue'
import router from './router'
// Element Plus 改为按需引入（见 vite.config.js 的 unplugin-vue-components）。
// 模板里的 el-xxx 组件会自动按需加载，这里只手动补上「命令式调用」用到的样式。
import 'element-plus/es/components/message/style/css'
import 'element-plus/es/components/message-box/style/css'
import 'element-plus/es/components/loading/style/css'
import './styles/tokens.css'
import BaseComponents from './components/base'
import { pinia } from './stores'
import { useAuthStore } from './stores/auth'
import { initTheme } from './utils/theme.js'

const app = createApp(App)
// 应用启动前先恢复个人页主题状态，避免切回个人页时闪一下默认色
initTheme()
app.use(BaseComponents)
app.use(pinia)
// 应用启动时同步初始化登录态，页面/路由守卫可立即读取
useAuthStore().init()
app.use(router)
app.mount('#app')
