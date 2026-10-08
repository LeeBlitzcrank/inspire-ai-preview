/**
 * 文件：frontend/tests/e2e/recommendation.spec.js
 * 所属模块：Playwright 端到端测试
 * 主要职责：验证个性化推荐理由和推荐反馈接口
 * 维护说明：推荐排序规则变化时同步维护本测试。
 * INSPIRE_FILE_HEADER
 */
import {expect, test} from '@playwright/test'

const API_BASE = process.env.E2E_API_BASE || 'http://127.0.0.1:8080/api'

async function login(request) {
  const response = await request.post(`${API_BASE}/auth/login`, {
    data: {username: 'user001', password: '112233'}
  })
  const body = await response.json()
  expect(body.code, body.msg).toBe(200)
  return body.data.accessToken
}

test('登录用户推荐包含个性化理由', async ({request}) => {
  const token = await login(request)
  const response = await request.get(`${API_BASE}/inspire/public/recommend`, {
    headers: {Authorization: `Bearer ${token}`},
    params: {page: 1, size: 5}
  })
  const body = await response.json()
  expect(body.code, body.msg).toBe(200)
  expect(body.data.length).toBeGreaterThan(0)
  expect(body.data.some(item => Boolean(item.recommendReason))).toBe(true)
})

test('推荐曝光和跳过反馈可以写入', async ({request}) => {
  const token = await login(request)
  const response = await request.get(`${API_BASE}/inspire/public/recommend`, {
    headers: {Authorization: `Bearer ${token}`},
    params: {page: 1, size: 1}
  })
  const inspireId = (await response.json()).data[0].id

  for (const eventType of ['IMPRESSION', 'SKIP']) {
    const event = await request.post(`${API_BASE}/inspire/recommend/event`, {
      headers: {Authorization: `Bearer ${token}`},
      data: {inspireId, eventType, reasonCode: 'e2e'}
    })
    expect((await event.json()).code).toBe(200)
  }
})

test('后台推荐管理和推送指标接口可用', async ({request}) => {
  const login = await request.post(`${API_BASE}/admin/login`, {
    data: {username: 'admin', password: '112233'}
  })
  const token = (await login.json()).data.token
  const headers = {Authorization: `Bearer ${token}`}

  const config = await request.get(`${API_BASE}/admin/recommend/config`, {headers})
  expect((await config.json()).code).toBe(200)

  const pushes = await request.get(`${API_BASE}/admin/recommend/push`, {headers})
  expect((await pushes.json()).code).toBe(200)

  const metrics = await request.get(`${API_BASE}/admin/recommend/metrics`, {headers})
  expect((await metrics.json()).code).toBe(200)
})

test('后台推荐管理页面可以打开', async ({page, request}) => {
  const login = await request.post(`${API_BASE}/admin/login`, {
    data: {username: 'admin', password: '112233'}
  })
  const token = (await login.json()).data.token
  await page.addInitScript(value => {
    sessionStorage.setItem('adminToken', value)
    sessionStorage.setItem('adminUser', 'admin')
  }, token)
  await page.goto('/#/admin/recommend')
  await expect(page.locator('.recommend-page')).toBeVisible()
  await expect(page.getByText('人工推送', {exact: true})).toBeVisible()
})
