/**
 * 双Token安全存储（文档 4.1.3 节）
 * - accessToken：内存存储，防止 XSS
 * - refreshToken：sessionStorage（当前标签页独立）
 * - 活跃时间戳：15 分钟无操作自动登出
 */

/** 内存中的 AccessToken */
let accessToken = null

let rememberMe = false

const REFRESH_KEY = 'inspire_refresh_token'
const ACCESS_KEY = 'inspire_access_token'
const ACTIVE_KEY = 'inspire_last_active'
const REMEMBER_KEY = 'inspire_remember_me'
const LONG_LIVED_KEY = 'inspire_long_lived_session'
const SESSION_TIMEOUT = 15 * 60 * 1000

/** 页面加载时从 sessionStorage 恢复记住的 accessToken */
function restoreRemembered() {
  if (sessionStorage.getItem(REMEMBER_KEY) === '1') {
    rememberMe = true
    const saved = sessionStorage.getItem(ACCESS_KEY)
    if (saved) accessToken = saved
  }
}
restoreRemembered()

export function saveTokens(tokens) {
  if (tokens.accessToken) {
    accessToken = tokens.accessToken
    if (rememberMe) sessionStorage.setItem(ACCESS_KEY, tokens.accessToken)
  }
  if (tokens.refreshToken) sessionStorage.setItem(REFRESH_KEY, tokens.refreshToken)
  sessionStorage.setItem(LONG_LIVED_KEY, tokens.longLived ? '1' : '0')
  syncLoginFlag()
  saveLastActive()
}

export function getAccessToken() { return accessToken }
export function getRefreshToken() { return sessionStorage.getItem(REFRESH_KEY) }
export function hasSession() { return !!accessToken || !!getRefreshToken() }

export function syncLoginFlag() {
  if (hasSession()) {
    sessionStorage.setItem('isLogin', '1')
  } else {
    sessionStorage.removeItem('isLogin')
  }
}

export function clearAllTokens() {
  accessToken = null
  sessionStorage.removeItem(REFRESH_KEY)
  sessionStorage.removeItem(ACCESS_KEY)
  sessionStorage.removeItem(REMEMBER_KEY)
  sessionStorage.removeItem(ACTIVE_KEY)
  sessionStorage.removeItem('token')
  sessionStorage.removeItem('adminToken')
  sessionStorage.removeItem('isLogin')
  sessionStorage.removeItem('userAccount')
  sessionStorage.removeItem('userId')
  sessionStorage.removeItem('adminUser')
  sessionStorage.removeItem(LONG_LIVED_KEY)
}

export function isLoggedIn() { return !!accessToken }

/** 记住账号模式：accessToken 持久化到 sessionStorage  */
export function setRememberMe(on) {
  rememberMe = on
  if (on) {
    sessionStorage.setItem(REMEMBER_KEY, '1')
    if (accessToken) sessionStorage.setItem(ACCESS_KEY, accessToken)
  } else {
    sessionStorage.removeItem(REMEMBER_KEY)
    sessionStorage.removeItem(ACCESS_KEY)
  }
}

/** 记录当前时间为最后活跃时间 */
export function saveLastActive() {
  sessionStorage.setItem(ACTIVE_KEY, Date.now().toString())
}

/** 检查是否超过 15 分钟无操作 */
export function isSessionExpired() {
  if (sessionStorage.getItem(LONG_LIVED_KEY) === '1') return false
  const ts = sessionStorage.getItem(ACTIVE_KEY)
  if (!ts) return false
  return Date.now() - parseInt(ts, 10) > SESSION_TIMEOUT
}
