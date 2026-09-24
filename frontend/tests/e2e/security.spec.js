import {expect, test} from '@playwright/test'

const API_BASE = process.env.E2E_API_BASE || 'http://127.0.0.1:8080/api'
const CORE_BASE = process.env.E2E_CORE_BASE || 'http://127.0.0.1:8083'

async function login(request, username = 'user001') {
  const response = await request.post(`${API_BASE}/auth/login`, {
    data: {username, password: '112233'}
  })
  const body = await response.json()
  expect(body.code, body.msg).toBe(200)
  return body.data.accessToken
}

async function createInspire(request, token, title) {
  const response = await request.post(`${API_BASE}/inspire`, {
    headers: {Authorization: `Bearer ${token}`},
    data: {
      title,
      content: '自动化安全与并发测试正文',
      tag: '生活',
      status: 1
    }
  })
  const body = await response.json()
  expect(body.code, body.msg).toBe(200)
  return String(body.data.id)
}

test('网关注入 traceId，伪造下游身份头被拒绝', async ({request}) => {
  const publicResponse = await request.get(`${API_BASE}/inspire/public/list?page=1&size=1`)
  expect(publicResponse.headers()['x-trace-id']).toMatch(/^[A-Za-z0-9-]{1,64}$/)

  const forged = await request.get(`${CORE_BASE}/inspire/my`, {
    headers: {
      'X-User-Id': '100000000000000001',
      'X-User-Role': 'admin'
    }
  })
  expect(forged.status()).toBe(401)
  expect((await forged.json()).code).toBe(401001)
})

test('SSRF 内网地址被拒绝', async ({request}) => {
  const proxy = await request.get(`${API_BASE}/file/poster-cover`, {
    params: {url: 'http://127.0.0.1:8080/actuator/health'}
  })
  expect(proxy.status()).toBe(403)

  const token = await login(request)
  const upload = await request.post(`${API_BASE}/file/upload-from-url`, {
    headers: {Authorization: `Bearer ${token}`},
    data: {url: 'http://127.0.0.1:8080/actuator/health'}
  })
  const body = await upload.json()
  expect(body.code).not.toBe(200)
})

test('越权修改、并发点赞收藏和缓存一致性', async ({request}) => {
  const token1 = await login(request, 'user001')
  const token2 = await login(request, 'user002')
  const id = await createInspire(request, token1, `安全测试${Date.now().toString().slice(-6)}`)

  try {
    const forbidden = await request.put(`${API_BASE}/inspire/${id}`, {
      headers: {Authorization: `Bearer ${token2}`},
      data: {title: '越权修改'}
    })
    expect((await forbidden.json()).code).not.toBe(200)

    const [like1, like2] = await Promise.all([
      request.post(`${API_BASE}/inspire/${id}/like`, {
        headers: {Authorization: `Bearer ${token2}`}
      }),
      request.post(`${API_BASE}/inspire/${id}/like`, {
        headers: {Authorization: `Bearer ${token2}`}
      })
    ])
    const likeCodes = await Promise.all([like1.json(), like2.json()])
    expect(likeCodes.filter(item => item.code === 200)).toHaveLength(1)

    const [collect1, collect2] = await Promise.all([
      request.post(`${API_BASE}/inspire/${id}/collect`, {
        headers: {Authorization: `Bearer ${token2}`}
      }),
      request.post(`${API_BASE}/inspire/${id}/collect`, {
        headers: {Authorization: `Bearer ${token2}`}
      })
    ])
    const collectCodes = await Promise.all([collect1.json(), collect2.json()])
    expect(collectCodes.filter(item => item.code === 200)).toHaveLength(1)

    const listAfterCreate = await request.get(`${API_BASE}/inspire/public/list`, {
      params: {page: 1, size: 5, sort: 'time'}
    })
    expect((await listAfterCreate.json()).data.some(item => String(item.id) === id)).toBeTruthy()
  } finally {
    await request.delete(`${API_BASE}/inspire/${id}/collect`, {
      headers: {Authorization: `Bearer ${token2}`}
    }).catch(() => {})
    await request.delete(`${API_BASE}/inspire/${id}/like`, {
      headers: {Authorization: `Bearer ${token2}`}
    }).catch(() => {})
    await request.delete(`${API_BASE}/inspire/${id}`, {
      headers: {Authorization: `Bearer ${token1}`}
    }).catch(() => {})
  }

  const listAfterDelete = await request.get(`${API_BASE}/inspire/public/list`, {
    params: {page: 1, size: 5, sort: 'time'}
  })
  expect((await listAfterDelete.json()).data.some(item => String(item.id) === id)).toBeFalsy()
})
