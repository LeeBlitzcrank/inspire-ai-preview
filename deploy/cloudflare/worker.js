// =============================================
// Cloudflare Worker —— 让前端域名 ai.20sherry.com 与后端 API 同源
//
// 作用：前端用相对路径 /api/* 和 /uploads/* 请求，Worker 把它们转发到
//       api.20sherry.com（背后是 Cloudflare Tunnel → 网关 8080）。
//       浏览器视为同源，不再发送 CORS 预检请求(OPTIONS)。
//
// Cloudflare 配置（Workers & Pages → 你的 Worker → Settings → Triggers → Routes）：
//   添加两条 Route：
//     ai.20sherry.com/api/*
//     ai.20sherry.com/uploads/*
//   （只匹配这两条，其它路径直接走 GitHub Pages，不会进入 Worker，无死循环）
// =============================================

const API_HOST = 'api.20sherry.com'
const PUBLIC_CACHE_TTL = [
  { prefix: '/api/inspire/public/categories', ttl: 3600 },
  { prefix: '/api/inspire/public/word-cloud', ttl: 3600 },
  { prefix: '/api/inspire/public/recommend', ttl: 300 },
  { prefix: '/api/inspire/public/list', ttl: 300 },
  { prefix: '/api/file/poster-cover', ttl: 86400 }
]

function publicCacheTtl(pathname) {
  const hit = PUBLIC_CACHE_TTL.find(item => pathname.startsWith(item.prefix))
  return hit ? hit.ttl : 0
}

export default {
  async fetch(request, env, ctx) {
    const url = new URL(request.url)
    // 只处理 API / 上传资源，其它路径原样放行
    if (url.pathname.startsWith('/api/') || url.pathname.startsWith('/uploads/')) {
      const ttl = request.method === 'GET' && !request.headers.get('Authorization')
        ? publicCacheTtl(url.pathname)
        : 0
      const cache = caches.default
      const cacheKey = new Request(url.toString(), { method: 'GET' })

      if (ttl > 0) {
        const cached = await cache.match(cacheKey)
        if (cached) {
          const headers = new Headers(cached.headers)
          headers.set('X-Worker-Cache', 'HIT')
          return new Response(cached.body, {
            status: cached.status,
            statusText: cached.statusText,
            headers
          })
        }
      }

      url.hostname = API_HOST
      url.port = ''
      const upstream = await fetch(new Request(url.toString(), request))

      if (ttl > 0 && upstream.status === 200) {
        const headers = new Headers(upstream.headers)
        headers.set('Cache-Control', `public, max-age=${ttl}`)
        headers.set('X-Worker-Cache', 'MISS')
        const response = new Response(upstream.body, {
          status: upstream.status,
          statusText: upstream.statusText,
          headers
        })
        ctx.waitUntil(cache.put(cacheKey, response.clone()))
        return response
      }

      return upstream
    }
    return fetch(request)
  }
}
