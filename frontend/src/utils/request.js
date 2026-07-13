import axios from 'axios'
import { ElMessage } from 'element-plus'
import { getAccessToken, getRefreshToken, clearAllTokens, saveTokens, saveLastActive, isSessionExpired } from './tokenStorage.js'

const API_BASE = import.meta.env.VITE_API_BASE ? import.meta.env.VITE_API_BASE + '/api' : '/api'
const service = axios.create({ baseURL: API_BASE, timeout: 15000 })

// ===== 自动刷新锁 =====
let isRefreshing = false
let pendingQueue = []

function resolvePending(newToken) {
  pendingQueue.forEach(cb => cb(newToken))
  pendingQueue = []
}

// ===== 生产环境图片 URL 替换 =====
function fixImageUrls(obj) {
  if (!obj || typeof obj !== 'object') return
  const replacer = import.meta.env.VITE_API_BASE
  if (!replacer) return
  for (const key of Object.keys(obj)) {
    const val = obj[key]
    if (typeof val === 'string' && val.startsWith('http://localhost:8083/')) {
      obj[key] = val.replace('http://localhost:8083', replacer)
    } else if (typeof val === 'string' && val.startsWith('/') && !val.startsWith('//')) {
      obj[key] = replacer + val
    } else if (Array.isArray(val)) {
      for (let i = 0; i < val.length; i++) {
        if (typeof val[i] === 'string' && val[i].startsWith('/') && !val[i].startsWith('//')) {
          val[i] = replacer + val[i]
        }
      }
    } else if (val && typeof val === 'object') {
      fixImageUrls(val)
    }
  }
}

// ===== 请求拦截器：无 token 时自动用 refreshToken 续期 =====
service.interceptors.request.use(async config => {
  // 检查 15 分钟无操作过期
  if (isSessionExpired()) {
    clearAllTokens()
    window.location.href = '/login'
    return Promise.reject(new Error('Session expired'))
  }

  let token = getAccessToken()

  // 内存无 accessToken 但 localStorage 有 refreshToken → 自动续期（页面刷新后）
  if (!token) {
    const rt = getRefreshToken()
    if (rt && !isRefreshing) {
      isRefreshing = true
      try {
        const res = await axios.post(`${API_BASE}/auth/refresh`, null, {
          headers: { 'Refresh-Token': rt },
          timeout: 5000
        })
        if (res.data.code === 200 && res.data.data) {
          saveTokens(res.data.data)
          token = res.data.data.accessToken
        }
      } catch {
        // 刷新失败，不阻塞请求
      } finally {
        isRefreshing = false
      }
    }
  }

  if (token) {
    config.headers.Authorization = `Bearer ${token}`
    saveLastActive()  // 有请求即算活跃，重置 15 分钟倒计时
  }
  return config
})

// ===== 响应拦截器 =====
service.interceptors.response.use(
  res => {
    const data = res.data
    if (import.meta.env.PROD) fixImageUrls(data)
    saveLastActive()
    return data
  },
  async err => {
    const res = err.response
    if (!res) {
      ElMessage.error('网络异常，请检查连接')
      return Promise.reject(err)
    }

    const data = res.data || {}
    const code = data.code
    const path = err.config?.url || ''

    // ===== 401004：AccessToken 过期 → 无感刷新 =====
    if (code === 401004) {
      const rt = getRefreshToken()
      if (!rt) {
        clearAllTokens()
        ElMessage.error('登录已过期，请重新登录')
        redirectToLogin(path)
        return Promise.reject(err)
      }

      if (!isRefreshing) {
        isRefreshing = true
        try {
          const refreshRes = await axios.post(`${API_BASE}/auth/refresh`, null, {
            headers: { 'Refresh-Token': rt }
          })
          const rd = refreshRes.data
          if (rd.code === 200 && rd.data) {
            saveTokens(rd.data)
            resolvePending(rd.data.accessToken)
            err.config.headers.Authorization = `Bearer ${rd.data.accessToken}`
            return service(err.config)
          }
        } catch {
          clearAllTokens()
          ElMessage.error('登录已过期，请重新登录')
          redirectToLogin(path)
          return Promise.reject(err)
        } finally {
          isRefreshing = false
        }
      } else {
        return new Promise(resolve => {
          pendingQueue.push(newToken => {
            err.config.headers.Authorization = `Bearer ${newToken}`
            resolve(service(err.config))
          })
        })
      }
    }

    // ===== 401001 / 401002 / 401003 / 401005 → 强制登录 =====
    if (code >= 401001 && code <= 401005) {
      clearAllTokens()
      ElMessage.error(data.msg || '登录已失效')
      redirectToLogin(path)
      return Promise.reject(err)
    }

    // ===== 403001：权限不足 =====
    if (code === 403001) {
      ElMessage.error('当前角色无权限执行此操作')
      return Promise.reject(err)
    }

    // ===== 通用错误 =====
    ElMessage.error(data.msg || '服务异常,请稍后重试')
    return Promise.reject(err)
  }
)

function redirectToLogin(path) {
  if (path.includes('/admin/')) {
    window.location.href = '/admin/login'
  } else {
    window.location.href = '/login'
  }
}

export default service
