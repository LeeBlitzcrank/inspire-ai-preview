import axios from 'axios'
import { ElMessage } from 'element-plus'

const service = axios.create({
  baseURL: '/api',
  timeout: 15000
})

service.interceptors.request.use(config => {
  const token = localStorage.getItem('adminToken') || localStorage.getItem('token')
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

service.interceptors.response.use(
    res => res.data,
    err => {
      ElMessage.error(err.response?.data?.msg || '服务异常')
      if (err.response?.status === 401) {
        localStorage.removeItem('token')
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
      }
      return Promise.reject(err)
    }
)
export default service
