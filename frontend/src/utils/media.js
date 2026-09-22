/**
 * 列表场景的图片取址：优先用缩略图，避免列表里直接加载原图。
 *
 * 两种来源分别处理：
 *  1. 本站上传图（/uploads/xxx.jpg）：上传流程已经生成了 400px 的 thumb_xxx.jpg，直接换前缀
 *  2. 图片 CDN（img.20sherry.com）：Nginx 装了 image_filter 模块，带 ?w= 会按需缩放 + 转 WebP
 *
 * 其他图源（比如 picsum）保持原样，不做处理。
 */
export function thumbOf(url, width = 400) {
  if (!url || typeof url !== 'string') return url

  // 1) 本站上传图 → 换成已生成好的缩略图
  const m = /^(.*\/uploads\/)(?!thumb_)([^/?]+)(\?.*)?$/.exec(url)
  if (m) return `${m[1]}thumb_${m[2]}${m[3] || ''}`

  // 2) 图片 CDN → 交给 Nginx 动态缩放
  if (url.includes('img.20sherry.com')) {
    return url + (url.includes('?') ? '&' : '?') + 'w=' + width
  }

  // 3) 站内取图接口（/api/file/view?key=...）→ 由后端 thumbnailator 出缩略图
  if (url.includes('/file/view')) {
    return url + (url.includes('?') ? '&' : '?') + 'w=' + width
  }

  return url
}

/**
 * 生成 srcset，让浏览器按设备像素比和显示宽度自己挑合适的图。
 * 目前只有两档可用：400px 缩略图 + 原图，所以只在两者确实不同时才返回。
 */
export function srcsetOf(url) {
  const thumb = thumbOf(url)
  if (!thumb || thumb === url) return undefined
  return `${thumb} 400w, ${url} 1200w`
}
