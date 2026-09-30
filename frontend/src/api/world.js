/**
 * 文件：frontend/src/api/world.js
 * 所属模块：前端接口请求封装
 * 主要职责：前端 API 模块，统一封装后端接口调用
 * 维护说明：注释解释文件边界和核心意图，具体业务规则以方法、组件和主文档说明为准。
 * INSPIRE_FILE_HEADER
 */
import request, {cachedGet, clearGetCache} from '@/utils/request.js'

export const getWorldSeeds = () => cachedGet('/world/public/seeds', undefined, 60 * 1000)
export const getWorldSeed = (seedId) => request.get(`/world/public/seeds/${seedId}`)
export const getWorldChapters = (branchId, beforeChapterNo, size = 10) =>
  request.get(`/world/public/branches/${branchId}/chapters`, {
    params: {
      beforeChapterNo: beforeChapterNo || undefined,
      size
    }
  })

const taskOptions = (idempotencyKey) => ({
  headers: {'Idempotency-Key': idempotencyKey}
})

export const submitWorldSeed = (data, idempotencyKey) =>
  request.post('/world/generate', data, taskOptions(idempotencyKey))

export const submitWorldChapter = (data, idempotencyKey) =>
  request.post('/world/chapter', data, taskOptions(idempotencyKey))

export const getWorldTask = (taskId) => request.get(`/world/tasks/${taskId}`)
export const voteWorld = (data) => request.post('/world/vote', data)

export const clearWorldSeedCache = () => clearGetCache()
