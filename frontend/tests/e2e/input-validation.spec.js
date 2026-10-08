/**
 * 文件：frontend/tests/e2e/input-validation.spec.js
 * 所属模块：Playwright 端到端测试
 * 主要职责：验证生产输入边界不能通过直接调用 API 绕过前端
 * 维护说明：测试依赖本地后端运行；认证输入规则变化时必须同步调整本测试。
 * INSPIRE_FILE_HEADER
 */
import {expect, test} from '@playwright/test'

const API_BASE = process.env.E2E_API_BASE || 'http://127.0.0.1:8080/api'

async function login(request, username = 'user001') {
  const response = await request.post(`${API_BASE}/auth/login`, {
    data: {username, password: '112233'}
  })
  const body = await response.json()
  expect(body.code, body.msg).toBe(200)
  return body.data.accessToken
}

test('绕过前端时，注册边界仍由后端拒绝', async ({request}) => {
  const badUsername = await request.post(`${API_BASE}/auth/register`, {
    data: {
      username: 'ab',
      email: 'validation@example.com',
      password: 'StrongPass123',
      confirmPassword: 'StrongPass123'
    }
  })
  expect((await badUsername.json()).code).toBe(400)

  const weakPassword = await request.post(`${API_BASE}/auth/register`, {
    data: {
      username: `user${Date.now().toString().slice(-8)}`,
      email: 'validation@example.com',
      password: '12345678',
      confirmPassword: '12345678'
    }
  })
  expect((await weakPassword.json()).code).toBe(400)
})

test('评论和私信超长内容由后端拒绝', async ({request}) => {
  const token = await login(request)
  const listResponse = await request.get(`${API_BASE}/inspire/public/list`, {
    params: {page: 1, size: 1, sort: 'time'}
  })
  const inspireId = (await listResponse.json()).data?.[0]?.id
  expect(inspireId).toBeTruthy()

  const commentResponse = await request.post(`${API_BASE}/inspire/${inspireId}/comment`, {
    headers: {Authorization: `Bearer ${token}`},
    data: {content: '评'.repeat(501)}
  })
  expect((await commentResponse.json()).code).toBe(400)

  const messageResponse = await request.post(`${API_BASE}/message/send`, {
    headers: {Authorization: `Bearer ${token}`},
    data: {
      toUserId: 100000000000000002,
      content: '消'.repeat(1001),
      type: 'text'
    }
  })
  expect((await messageResponse.json()).code).toBe(400)
})

test('手机号验证码可以完成登录或自动注册', async ({request}) => {
  const phone = `139${String(Date.now()).slice(-8)}`
  const send = await request.post(`${API_BASE}/auth/sms/send`, {
    data: {phone, purpose: 'login'}
  })
  const sendBody = await send.json()
  expect(sendBody.code, sendBody.msg).toBe(200)
  expect(sendBody.data.devCode).toMatch(/^\d{6}$/)

  const loginResponse = await request.post(`${API_BASE}/auth/sms/login`, {
    data: {phone, code: sendBody.data.devCode}
  })
  const loginBody = await loginResponse.json()
  expect(loginBody.code, loginBody.msg).toBe(200)
  expect(loginBody.data.accessToken).toBeTruthy()
  expect(loginBody.data.passwordUpgradeRequired).toBe(false)
})

test('短信冷却期间按钮实时倒计时并保持禁用', async ({page, request}) => {
  const phone = `136${String(Date.now()).slice(-8)}`
  const first = await request.post(`${API_BASE}/auth/sms/send`, {
    data: {phone, purpose: 'login'}
  })
  expect((await first.json()).code).toBe(200)

  await page.goto('/#/login')
  await page.getByRole('button', {name: '手机号验证码'}).click()
  await page.locator('input[placeholder="请输入11位手机号"]').fill(phone)
  const sendButton = page.locator('.sms-send-button')
  await sendButton.click()

  await expect(sendButton).toContainText('后重发')
  await expect(sendButton).toBeDisabled()
  await expect(page.locator('.el-message--error')).toHaveCount(0)

  await page.reload()
  await expect(page.locator('.sms-send-button')).toContainText('后重发')
  await expect(page.locator('.sms-send-button')).toBeDisabled()
})
