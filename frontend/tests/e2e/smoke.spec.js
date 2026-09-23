import {expect, test} from '@playwright/test'

const PNG_1X1 = Buffer.from(
  'iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNk+A8AAQUBAScY42YAAAAASUVORK5CYII=',
  'base64'
)

const USERNAME = process.env.E2E_USER || 'user001'
const PASSWORD = process.env.E2E_PASSWORD || '112233'
const API_BASE = process.env.E2E_API_BASE || 'http://127.0.0.1:8080/api'

test('登录、发布、上传、评论主链路', async ({ page, request }) => {
  await page.addInitScript(() => {
    sessionStorage.setItem('__disable_web_vitals__', '1')
  })

  let createdInspireId = ''
  const title = `冒烟测试${Date.now().toString().slice(-6)}`
  const comment = `冒烟评论${Date.now().toString().slice(-4)}`

  try {
    await page.goto('/#/login')
    await page.getByPlaceholder('请输入账号').fill(USERNAME)
    await page.getByPlaceholder('请输入密码').fill(PASSWORD)
    await page.getByRole('button', { name: '立即登录' }).click()
    await expect(page).not.toHaveURL(/\/login/)

    await page.goto('/#/create')
    await page.getByPlaceholder('输入简短标题…').fill(title)
    await page.locator('.chips .chip').first().click()
    await page.locator('.editable').fill('这是一条本地 Playwright 冒烟测试正文，不调用 AI。')

    const uploadResponsePromise = page.waitForResponse(response =>
      response.url().includes('/api/file/upload')
      && response.request().method() === 'POST'
    )
    await page.locator('input[type="file"]').setInputFiles({
      name: 'smoke.png',
      mimeType: 'image/png',
      buffer: PNG_1X1
    })
    const uploadResponse = await uploadResponsePromise
    const uploadJson = await uploadResponse.json()
    expect(uploadJson.code).toBe(200)
    expect(uploadJson.data.url).toContain('/uploads/')

    const createResponsePromise = page.waitForResponse(response => {
      const url = new URL(response.url())
      return url.pathname === '/api/inspire'
        && response.request().method() === 'POST'
    })
    await page.getByRole('button', { name: '发布' }).click()
    const createResponse = await createResponsePromise
    const createJson = await createResponse.json()
    expect(createJson.code, createJson.msg).toBe(200)
    createdInspireId = String(createJson.data.id)

    await page.goto(`/#/detail/${createdInspireId}`)
    const commentInput = page.getByPlaceholder('说点什么，回车发送')
    await expect(commentInput).toBeVisible()
    await commentInput.fill(comment)

    const commentResponsePromise = page.waitForResponse(response =>
      response.url().includes(`/api/inspire/${createdInspireId}/comment`)
      && response.request().method() === 'POST'
    )
    await commentInput.press('Enter')
    const commentResponse = await commentResponsePromise
    const commentJson = await commentResponse.json()
    expect(commentJson.code, commentJson.msg).toBe(200)
    await expect(page.getByText(comment)).toBeVisible()
  } finally {
    if (createdInspireId) {
      const token = await page.evaluate(() => sessionStorage.getItem('token')).catch(() => '')
      if (token) {
        await request.delete(`${API_BASE}/inspire/${createdInspireId}`, {
          headers: { Authorization: `Bearer ${token}` }
        }).catch(() => {})
      }
    }
  }
})
