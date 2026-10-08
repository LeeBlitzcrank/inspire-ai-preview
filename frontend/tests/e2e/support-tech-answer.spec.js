/**
 * 文件：frontend/tests/e2e/support-tech-answer.spec.js
 * 所属模块：Playwright 端到端测试
 * 主要职责：验证客服能回答技术栈和架构优势
 * 维护说明：客服知识库中技术栈章节变化时同步维护本测试。
 * INSPIRE_FILE_HEADER
 */
import {expect, test} from '@playwright/test'

const API_BASE = process.env.E2E_API_BASE || 'http://127.0.0.1:8080/api'

test('客服能综合回答技术栈和架构优势', async ({request}) => {
  const response = await request.post(`${API_BASE}/rag/support/ask`, {
    data: {query: '这个项目的技术栈和优势是什么？', topK: 8}
  })
  const body = await response.json()
  expect(body.code, body.msg).toBe(200)
  expect(body.data.answer).toContain('Vue 3')
  expect(body.data.answer).toContain('Spring Boot')
  expect(body.data.answer).toContain('RocketMQ')
  expect(body.data.answer).toContain('A/B')
})

test('客服能回答业务流程且不把优势段落串入推荐问题', async ({request}) => {
  const flow = await request.post(`${API_BASE}/rag/support/ask`, {
    data: {query: '业务流程', topK: 8}
  })
  const flowBody = await flow.json()
  expect(flowBody.code, flowBody.msg).toBe(200)
  expect(flowBody.data.answer).toContain('完整业务闭环')
  expect(flowBody.data.answer).toContain('RocketMQ')

  const recommend = await request.post(`${API_BASE}/rag/support/ask`, {
    data: {query: '推荐系统是怎么实现的', topK: 8}
  })
  const recommendBody = await recommend.json()
  expect(recommendBody.code, recommendBody.msg).toBe(200)
  expect(recommendBody.data.answer).toContain('推荐系统包含行为采集')
  expect(recommendBody.data.answer).not.toContain('项目不是单页面演示')
})
