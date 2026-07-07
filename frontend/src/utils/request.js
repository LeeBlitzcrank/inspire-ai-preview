import axios from 'axios'
import { ElMessage } from 'element-plus'

// 生产环境使用内网穿透地址，开发环境使用 Vite proxy
const API_BASE = import.meta.env.VITE_API_BASE ? import.meta.env.VITE_API_BASE + '/api'  : '/api'

const service = axios.create({
  baseURL: API_BASE,
  timeout: 15000
})

// 生产环境下，将后端返回的 localhost:8083 图片 URL 替换为穿透地址
function fixImageUrls(obj) {
  if (!obj || typeof obj !== 'object') return
  const replacer = import.meta.env.VITE_API_BASE
  if (!replacer) return
  for (const key of Object.keys(obj)) {
    const val = obj[key]
    if (typeof val === 'string' && val.startsWith('http://localhost:8083/')) {
      obj[key] = val.replace('http://localhost:8083', replacer)
    } else if (Array.isArray(val)) {
      val.forEach(fixImageUrls)
    } else if (val && typeof val === 'object') {
      fixImageUrls(val)
    }
  }
}

service.interceptors.request.use(config => {
  const isAdminApi = config.url && config.url.includes('/admin/')
  const token = isAdminApi ? localStorage.getItem('adminToken') : localStorage.getItem('token')
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

service.interceptors.response.use(
    res => {
      const data = res.data
      // 生产环境修复图片 URL
      if (import.meta.env.PROD) fixImageUrls(data)
      return data
    },
    err => {
      ElMessage.error(err.response?.data?.msg || '服务异常')
      if (err.response?.status === 401) {
        localStorage.removeItem('token')
        localStorage.removeItem('adminToken')
        localStorage.removeItem('isLogin')
        localStorage.removeItem('userAccount')
        localStorage.removeItem('userId')
        localStorage.removeItem('adminToken')
        localStorage.removeItem('adminUser')
        if (err.config && err.config.url && err.config.url.includes('/admin/')) {
          window.location.href = '/admin/login'
        } else {
          window.location.href = '/login'
        }
      } else if (err.response?.status >= 500 && err.response?.status < 600) {
        if (!err.config.url.includes('/admin/')) {
          window.location.href = '/500'
        }
      }
      return Promise.reject(err)
    }
)
export default service
