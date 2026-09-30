/**
 * 文件：frontend/src/api/rag.js
 * 所属模块：前端接口请求封装
 * 主要职责：前端 API 模块，统一封装后端接口调用
 * 维护说明：注释解释文件边界和核心意图，具体业务规则以方法、组件和主文档说明为准。
 * INSPIRE_FILE_HEADER
 */
import request from '@/utils/request.js'

export const askRag = (data) => request.post('/rag/ask', data, { timeout: 60000 })
export const searchRag = (data) => request.post('/rag/search', data, { timeout: 60000 })
export const getRagStatus = () => request.get('/rag/status')
