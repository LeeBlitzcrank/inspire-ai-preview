// =============================================
// JWT 双Token 认证 API（文档第4章各流程）
// =============================================
// 存储规范（文档 4.1.3 节）：
// - accessToken：内存存储（tokenStorage.js）
// - refreshToken：localStorage
// =============================================

import request from '@/utils/request.js'
import { getRefreshToken, saveTokens, clearAllTokens } from '@/utils/tokenStorage.js'

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
