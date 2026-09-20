// =============================================
// 全局登录态 store（方案C）
// 应用启动时一次性从 localStorage 初始化，
// 路由守卫 / 页面挂载前即可同步判断登录态，避免等接口返回才跳转。
// =============================================
import { defineStore } from 'pinia'

export const useAuthStore = defineStore('auth', {
  state: () => ({
    initialized: false,
    isLogin: false,
    userId: null
  }),
  actions: {
    /** 应用启动时调用一次，从 localStorage 同步登录态 */
    init() {
      if (this.initialized) return
      this.isLogin = localStorage.getItem('isLogin') === '1'
      this.userId = localStorage.getItem('userId') || null
      this.initialized = true
    },
    /** 登录成功后调用 */
    setLoggedIn() {
      this.isLogin = true
      this.userId = localStorage.getItem('userId') || null
      this.initialized = true
    },
    /** 登出 / 401 后调用 */
    setLoggedOut() {
      this.isLogin = false
      this.userId = null
      this.initialized = true
    }
  }
})
