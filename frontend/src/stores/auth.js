/**
 * 文件：frontend/src/stores/auth.js
 * 所属模块：Pinia 全局状态
 * 主要职责：Pinia 状态模块，管理跨页面共享状态
 * 维护说明：注释解释文件边界和核心意图，具体业务规则以方法、组件和主文档说明为准。
 * INSPIRE_FILE_HEADER
 */
// =============================================
// 全局登录态 store（方案C）
// 应用启动时一次性从 sessionStorage 初始化，
// 路由守卫 / 页面挂载前即可同步判断登录态，避免等接口返回才跳转。
// =============================================
import {defineStore} from 'pinia'
import {hasSession, syncLoginFlag} from '@/utils/tokenStorage.js'
import {bootstrapSession} from '@/api/auth.js'

export const useAuthStore = defineStore('auth', {
  state: () => ({
    initialized: false,
    isLogin: false,
    userId: null
  }),
  actions: {
    /** 应用启动时调用一次，从 sessionStorage 同步登录态 */
    init() {
      if (this.initialized) return
      this.isLogin = sessionStorage.getItem('isLogin') === '1' && hasSession()
      if (!this.isLogin) {
        sessionStorage.removeItem('isLogin')
      }
      this.userId = sessionStorage.getItem('userId') || null
      this.initialized = true
    },
    async bootstrap() {
      this.init()
      const restored = await bootstrapSession()
      this.isLogin = restored
      this.userId = sessionStorage.getItem('userId') || null
      this.initialized = true
      return restored
    },
    /** 登录成功后调用 */
    setLoggedIn() {
      this.isLogin = true
      syncLoginFlag()
      this.userId = sessionStorage.getItem('userId') || null
      this.initialized = true
    },
    /** 登出 / 401 后调用 */
    setLoggedOut() {
      this.isLogin = false
      this.userId = null
      sessionStorage.removeItem('isLogin')
      this.initialized = true
    }
  }
})
