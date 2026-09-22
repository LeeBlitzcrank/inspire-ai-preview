import axios from 'axios'
import { ElMessage } from './uiFeedback.js'
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
  // 后台管理接口走独立的管理员令牌，不参与前台登录态的过期判断
  const reqUrl = config.url || ''
  if (reqUrl.startsWith('/admin/') && !reqUrl.startsWith('/admin/public/')) {
    const adminToken = sessionStorage.getItem('adminToken')
    if (adminToken) {
      config.headers.Authorization = `Bearer ${adminToken}`
      return config
    }
  }

  // 登录/注册等接口本身不需要登录态，跳过「15 分钟无操作过期」检查，
  // 否则长时间未操作后第一次点登录会被先清空会话并跳回登录页
  const AUTH_FREE_PATHS = [
    '/auth/login', '/auth/register', '/auth/refresh',
    '/auth/forgot-password', '/auth/reset-password'
  ]
  const isAuthRequest = AUTH_FREE_PATHS.some(p => reqUrl.startsWith(p))

  // 检查 15 分钟无操作过期
  if (!isAuthRequest && isSessionExpired()) {
    clearAllTokens()
    window.location.href = '/#/login'
    return Promise.reject(new Error('Session expired'))
  }

  let token = getAccessToken()

  // 内存无 accessToken 但 sessionStorage 有 refreshToken → 自动续期（页面刷新后 / accessToken 过期后）
  if (!token) {
    const rt = getRefreshToken()
    if (rt) {
      if (isRefreshing) {
        // 关键修复：已经有刷新在飞时必须挂起等待，不能裸发无 token 的请求。
        // 否则并发请求会带上空 Authorization，被网关判为「未携带登录令牌」，
        // 进而触发前端清空登录态并跳回登录页。
        token = await new Promise(resolve => pendingQueue.push(resolve))
      } else {
        isRefreshing = true
        try {
          const res = await axios.post(`${API_BASE}/auth/refresh`, null, {
            headers: { 'Refresh-Token': rt },
            timeout: 8000
          })
          if (res.data.code === 200 && res.data.data) {
            saveTokens(res.data.data)
            token = res.data.data.accessToken
          }
        } catch {
          // 刷新失败：token 保持为空，唤醒等待者，后续按未登录处理
        } finally {
          isRefreshing = false
          resolvePending(token)
        }
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
    const cfg = err.config || {}
    // GET 是幂等的：网络抖动或服务端 5xx 时自动重试一次，避免偶发失败直接弹错误
    const status = err.response?.status
    const isGet = String(cfg.method || '').toLowerCase() === 'get'
    if (isGet && !cfg.__retried && (!err.response || status >= 500)) {
      cfg.__retried = true
      await new Promise(resolve => setTimeout(resolve, 600))
      return service(cfg)
    }

    const res = err.response
    if (!res) {
      ElMessage.error('网络异常，请检查连接')
      return Promise.reject(err)
    }

    const data = res.data || {}
    const code = data.code
    const path = err.config?.url || ''

    // ===== 缺令牌 / 无效令牌 / AccessToken 过期 → 有 refreshToken 就无感刷新一次 =====
    if (code === 401001 || code === 401002 || code === 401004) {
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
    window.location.href = '/#/admin/login'
  } else {
    window.location.href = '/#/login'
  }
}

export default service

// ============================================================================
// GET 请求内存缓存 + 并发去重
// 用于「分类」「词云」这类基本不变、又在多个页面被反复请求的公开数据：
//   - 5 分钟内重复请求直接命中内存，切页面零等待
//   - 同一时刻的并发请求只发一次，其余复用同一个 Promise
// 后台修改数据后请调用 clearGetCache() 让缓存立即失效。
// ============================================================================
const GET_CACHE_TTL = 5 * 60 * 1000
const getCacheMap = new Map()   // key -> { time, payload }
const inflightMap = new Map()   // key -> Promise

function buildKey(url, params) {
  return url + '?' + JSON.stringify(params || {})
}

export function cachedGet(url, params, ttl = GET_CACHE_TTL) {
  const key = buildKey(url, params)
  const hit = getCacheMap.get(key)
  if (hit && Date.now() - hit.time < ttl) {
    return Promise.resolve(hit.payload)
  }
  if (inflightMap.has(key)) {
    return inflightMap.get(key)
  }
  const p = service.get(url, { params })
    .then(payload => {
      getCacheMap.set(key, { time: Date.now(), payload })
      return payload
    })
    .finally(() => inflightMap.delete(key))
  inflightMap.set(key, p)
  return p
}

/** 后台改完分类/词云后调用，避免前台还看到 5 分钟内的旧数据 */
export function clearGetCache() {
  getCacheMap.clear()
}
