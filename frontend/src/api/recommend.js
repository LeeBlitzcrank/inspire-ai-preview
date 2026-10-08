import request from '@/utils/request.js'

export const adminRecommendConfig = () => request.get('/admin/recommend/config')
export const adminRecommendUpdateConfig = (data) => request.put('/admin/recommend/config', data)
export const adminRecommendPushList = () => request.get('/admin/recommend/push')
export const adminRecommendPushCreate = (data) => request.post('/admin/recommend/push', data)
export const adminRecommendPushUpdate = (id, data) => request.put(`/admin/recommend/push/${id}`, data)
export const adminRecommendPushStatus = (id, status) =>
  request.post(`/admin/recommend/push/${id}/status`, null, {params: {status}})
export const adminRecommendMetrics = () => request.get('/admin/recommend/metrics')
