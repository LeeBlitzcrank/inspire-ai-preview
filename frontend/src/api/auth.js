// =============================================
// JWT 双Token 认证 API（文档第4章各流程）
// =============================================
// 存储规范（文档 4.1.3 节）：
// - accessToken：内存存储（tokenStorage.js）
// - refreshToken：localStorage
// =============================================

import request from '@/utils/request.js'
import axios from 'axios'
import {
  getAccessToken,
  getRefreshToken,
  hasSession,
  saveTokens,
  clearAllTokens,
  syncLoginFlag
} from '@/utils/tokenStorage.js'

const API_BASE = import.meta.env.VITE_API_BASE ? import.meta.env.VITE_API_BASE + '/api' : '/api'
let bootstrapPromise = null

/**
 * 页面启动恢复会话：
 * accessToken 只在内存里，新标签页/刷新后如果只有 refreshToken，先刷新一次再挂载页面。
 * 同一标签内并发初始化复用同一个 Promise，不会重复请求。
 */
export function bootstrapSession() {
  if (getAccessToken() || !getRefreshToken()) {
    syncLoginFlag()
    return Promise.resolve(hasSession())
  }
  if (bootstrapPromise) return bootstrapPromise

  bootstrapPromise = axios.post(`${API_BASE}/auth/refresh`, null, {
    headers: { 'Refresh-Token': getRefreshToken() },
    timeout: 8000
  }).then(res => {
    if (res.data?.code === 200 && res.data.data) {
      saveTokens(res.data.data)
      syncLoginFlag()
      return true
    }
    clearAllTokens()
    return false
  }).catch(() => {
    clearAllTokens()
    return false
  }).finally(() => {
    bootstrapPromise = null
  })
  return bootstrapPromise
}

// ========== API 接口 ==========

/**
 * 用户登录（文档流程一）
 * 成功后自动保存双Token
 */
export function login(data) {
  return request.post('/auth/login', data).then(res => {
    if (res.code === 200 && res.data) {
      saveTokens(res.data)
    }
    return res
})
}

/**
 * 用户注册（自动登录并保存双Token）
 * 后端 /auth/register 返回的双Token结构与 login 一致
 */
export function register(data) {
  return request.post('/auth/register', data).then(res => {
    if (res.code === 200 && res.data) {
      saveTokens(res.data)
    }
    return res
  })
}

/**
 * 无感刷新 AccessToken（文档流程三）
 */
export function refreshToken() {
  const rt = getRefreshToken()
  if (!rt) return Promise.reject(new Error('No refresh token'))
  return request.post('/auth/refresh', null, {
    headers: { 'Refresh-Token': rt }
  }).then(res => {
    if (res.code === 200 && res.data) {
      saveTokens(res.data)
    }
    return res
  })
}

/**
 * 用户登出（文档流程四）
 * 成功后清除所有本地令牌
 */
export function logout() {
  const rt = getRefreshToken()
  return request.post('/auth/logout', { refreshToken: rt }).then(res => {
    clearAllTokens()
    return res
  })
}

/**
 * 后台强制下线（文档流程五，管理员）
 */
export function kickUser(userId) {
  return request.post(`/auth/admin/kick/${userId}`)
}
