/**
 * 双Token安全存储（文档 4.1.3 节）
 * - accessToken：内存存储，防止 XSS
 * - refreshToken：localStorage（记住账号模式额外持久化 accessToken）
 * - 活跃时间戳：15 分钟无操作自动登出
 */

/** 内存中的 AccessToken */
let accessToken = null

let rememberMe = false

const REFRESH_KEY = 'inspire_refresh_token'
const ACCESS_KEY = 'inspire_access_token'
const ACTIVE_KEY = 'inspire_last_active'
const REMEMBER_KEY = 'inspire_remember_me'
const SESSION_TIMEOUT = 15 * 60 * 1000

/** 页面加载时从 localStorage 恢复记住的 accessToken */
function restoreRemembered() {
  if (localStorage.getItem(REMEMBER_KEY) === '1') {
    rememberMe = true
    const saved = localStorage.getItem(ACCESS_KEY)
    if (saved) accessToken = saved
  }
}
restoreRemembered()

export function saveTokens(tokens) {
  if (tokens.accessToken) {
    accessToken = tokens.accessToken
    if (rememberMe) localStorage.setItem(ACCESS_KEY, tokens.accessToken)
  }
  if (tokens.refreshToken) localStorage.setItem(REFRESH_KEY, tokens.refreshToken)
  saveLastActive()
}

export function getAccessToken() { return accessToken }
export function getRefreshToken() { return localStorage.getItem(REFRESH_KEY) }

export function clearAllTokens() {
  accessToken = null
  localStorage.removeItem(REFRESH_KEY)
  localStorage.removeItem(ACCESS_KEY)
  localStorage.removeItem(REMEMBER_KEY)
  localStorage.removeItem(ACTIVE_KEY)
  localStorage.removeItem('token')
  localStorage.removeItem('adminToken')
  localStorage.removeItem('isLogin')
  localStorage.removeItem('userAccount')
  localStorage.removeItem('userId')
  localStorage.removeItem('adminUser')
}

export function isLoggedIn() { return !!accessToken }

/** 记住账号模式：accessToken 持久化到 localStorage  */
export function setRememberMe(on) {
  rememberMe = on
  if (on) {
    localStorage.setItem(REMEMBER_KEY, '1')
    if (accessToken) localStorage.setItem(ACCESS_KEY, accessToken)
  } else {
    localStorage.removeItem(REMEMBER_KEY)
    localStorage.removeItem(ACCESS_KEY)
  }
}

/** 记录当前时间为最后活跃时间 */
export function saveLastActive() {
  localStorage.setItem(ACTIVE_KEY, Date.now().toString())
}

/** 检查是否超过 15 分钟无操作 */
export function isSessionExpired() {
  const ts = localStorage.getItem(ACTIVE_KEY)
  if (!ts) return false
  return Date.now() - parseInt(ts, 10) > SESSION_TIMEOUT
}
