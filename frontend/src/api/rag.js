import request from '@/utils/request.js'

export const askRag = (data) => request.post('/rag/ask', data, { timeout: 60000 })
export const searchRag = (data) => request.post('/rag/search', data, { timeout: 60000 })
export const getRagStatus = () => request.get('/rag/status')
