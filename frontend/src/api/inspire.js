import request from '@/utils/request'

// ===== 用户认证 =====
export const login = (data) => request.post('/auth/login', data)
export const register = (data) => request.post('/auth/register', data)
export const getUserInfo = () => request.get('/auth/userinfo')
export const updateUserInfo = (data) => request.put('/auth/userinfo', data)
export const changePassword = (data) => request.put('/auth/password', data)
export const doLogout = () => request.post('/auth/logout')

// ===== 灵感核心 =====
export const getInspireList = (params) => request.get('/inspire/public/list', { params })
export const getInspireDetail = (id) => request.get(`/inspire/public/${id}`)
export const getMyInspires = () => request.get('/inspire/my')
export const getMyDrafts = () => request.get('/inspire/my/drafts')
export const getMyCollects = () => request.get('/inspire/my/collects')
export const createInspire = (data) => request.post('/inspire', data)
export const updateInspire = (id, data) => request.put(`/inspire/${id}`, data)
export const deleteInspire = (id) => request.delete(`/inspire/${id}`)
export const collectInspire = (id) => request.post(`/inspire/${id}/collect`)
export const uncollectInspire = (id) => request.delete(`/inspire/${id}/collect`)
export const likeInspire = (id) => request.post(`/inspire/${id}/like`)
export const unlikeInspire = (id) => request.delete(`/inspire/${id}/like`)

// ===== AI 创作 =====
export const aiGenerate = (data) => request.post('/ai/generate', data)
export const aiSelect = (data) => request.post('/ai/select', data)
export const aiPublish = (data) => request.post('/ai/publish', data)

// ===== 搜索 =====
export const searchInspires = (params) => request.get('/search/public', { params })

// ===== 忘记密码 =====
export const forgotPassword = (data) => request.post('/auth/forgot-password', data)
export const resetPassword = (data) => request.post('/auth/reset-password', data)
