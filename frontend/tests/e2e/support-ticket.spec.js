/**
 * 文件：frontend/tests/e2e/support-ticket.spec.js
 * 所属模块：Playwright 端到端测试
 * 主要职责：验证客服转人工工单和后台回复闭环
 * 维护说明：测试依赖本地后端运行；人工工单接口或页面入口变化时必须同步调整本测试。
 * INSPIRE_FILE_HEADER
 */
import {expect, test} from '@playwright/test'

const API_BASE = process.env.E2E_API_BASE || 'http://127.0.0.1:8080/api'

test('客服输入人工后打开转人工提交面板', async ({page}) => {
  await page.goto('/#/login')
  await expect(page.getByRole('button', {name: '打开AI客服'})).toBeVisible()
  await page.goto('/#/')
  await expect(page.getByRole('button', {name: '打开AI客服'})).toHaveCount(0)
  await page.goto('/#/login')
  const launcher = page.getByRole('button', {name: '打开AI客服'})
  await expect(launcher.locator('.launcher-copy')).toBeHidden()
  await launcher.hover()
  await expect(launcher.locator('.launcher-copy')).toBeVisible()
  await launcher.click()
  await page.getByPlaceholder('例如：怎么发布一篇灵感？').fill('人工')
  await page.getByRole('button', {name: '发送'}).click()
  await page.getByRole('button', {name: '打开转人工服务'}).click()

  await expect(page.locator('.support-sheet')).toBeVisible()
  await expect(page.locator('.support-sheet').getByText('转人工服务', {exact: true})).toBeVisible()
  await expect(page.getByPlaceholder('请描述具体问题、操作步骤和报错现象')).toHaveValue('人工')
})

test('AI客服可以拖动并记住位置', async ({page}) => {
  await page.goto('/#/login')
  const launcher = page.getByRole('button', {name: '打开AI客服'})
  const before = await launcher.boundingBox()
  expect(before).toBeTruthy()

  await page.mouse.move(before.x + before.width / 2, before.y + before.height / 2)
  await page.mouse.down()
  await page.mouse.move(before.x - 90, before.y - 110, {steps: 8})
  await page.mouse.up()

  const after = await launcher.boundingBox()
  expect(after.x).toBeLessThan(before.x - 40)
  expect(after.y).toBeLessThan(before.y - 50)

  await page.reload()
  const restored = await page.getByRole('button', {name: '打开AI客服'}).boundingBox()
  expect(Math.abs((restored.x + restored.width) - (after.x + after.width))).toBeLessThan(3)
  expect(Math.abs(restored.y - after.y)).toBeLessThan(3)
})

test('人工工单可由后台认领并回复', async ({request}) => {
  const createdResponse = await request.post(`${API_BASE}/rag/support/handoff`, {
    data: {
      issue: `E2E 工单 ${Date.now()}`,
      contact: 'e2e@example.com',
      attachments: []
    }
  })
  const created = await createdResponse.json()
  expect(created.code, created.msg).toBe(200)
  expect(created.data.ticketNo).toMatch(/^ST/)
  expect(created.data.accessToken).toBeTruthy()

  const adminLogin = await request.post(`${API_BASE}/admin/login`, {
    data: {username: 'admin', password: '112233'}
  })
  const adminBody = await adminLogin.json()
  expect(adminBody.code, adminBody.msg).toBe(200)
  const adminToken = adminBody.data.token

  const listResponse = await request.get(`${API_BASE}/admin/support/list`, {
    headers: {Authorization: `Bearer ${adminToken}`},
    params: {page: 1, size: 100}
  })
  const listBody = await listResponse.json()
  const ticket = listBody.data.list.find(item => item.ticketNo === created.data.ticketNo)
  expect(ticket).toBeTruthy()

  const claimResponse = await request.post(`${API_BASE}/admin/support/${ticket.id}/claim`, {
    headers: {Authorization: `Bearer ${adminToken}`}
  })
  expect((await claimResponse.json()).code).toBe(200)

  const replyResponse = await request.post(`${API_BASE}/admin/support/${ticket.id}/reply`, {
    headers: {Authorization: `Bearer ${adminToken}`},
    data: {content: '已收到，请补充完整报错信息。'}
  })
  expect((await replyResponse.json()).code).toBe(200)

  const userDetail = await request.get(
    `${API_BASE}/rag/support/ticket/${created.data.ticketNo}`,
    {headers: {'X-Support-Ticket-Token': created.data.accessToken}}
  )
  const userBody = await userDetail.json()
  expect(userBody.code, userBody.msg).toBe(200)
  expect(userBody.data.status).toBe('REPLIED')
  expect(userBody.data.lastReply).toContain('已收到')
})
