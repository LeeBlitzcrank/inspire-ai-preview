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

export default {
  async fetch(request) {
    const url = new URL(request.url)
    // 只处理 API / 上传资源，其它路径原样放行
    if (url.pathname.startsWith('/api/') || url.pathname.startsWith('/uploads/')) {
      url.hostname = API_HOST
      url.port = ''
      return fetch(new Request(url.toString(), request))
    }
    return fetch(request)
  }
}
