import {ref} from 'vue'
import {ElMessage} from '@/utils/uiFeedback.js'

const API_BASE = import.meta.env.VITE_API_BASE ? import.meta.env.VITE_API_BASE + '/api' : '/api'
const posterCoverProxy = (url) => `${API_BASE}/file/poster-cover?url=${encodeURIComponent(url)}`

export function useSharePoster({detail, imageList, isVideo, tagList, publishText, showSharePanel, DEFAULT_DESC}) {
  const posterVisible = ref(false)
  const posterUrl = ref('')
  const posterBuilding = ref(false)
  const posterTemplatePickerVisible = ref(false)
  const selectedPosterTemplate = ref('paper')
  const posterTemplates = [
    { id: 'paper', name: '文艺纸感', description: '纸张底色 · 衬线标题' },
    { id: 'minimal', name: '极简留白', description: '白底 · 大留白' },
    { id: 'magazine', name: '杂志封面', description: '大图 · 视觉冲击' }
  ]

  /* ==================== 分享海报（纯前端 Canvas 生成） ==================== */

  const POSTER_W = 1080
  const POSTER_H = 1440

  /** 以 cover 方式把图片裁切铺满目标区域 */
  const drawImageCover = (ctx, img, x, y, w, h) => {
    const scale = Math.max(w / img.width, h / img.height)
    const sw = w / scale
    const sh = h / scale
    const sx = (img.width - sw) / 2
    const sy = (img.height - sh) / 2
    ctx.drawImage(img, sx, sy, sw, sh, x, y, w, h)
  }

  const roundRectPath = (ctx, x, y, w, h, r) => {
    ctx.beginPath()
    ctx.moveTo(x + r, y)
    ctx.arcTo(x + w, y, x + w, y + h, r)
    ctx.arcTo(x + w, y + h, x, y + h, r)
    ctx.arcTo(x, y + h, x, y, r)
    ctx.arcTo(x, y, x + w, y, r)
    ctx.closePath()
  }

  /** 按最大宽度折行，超出 maxLines 时补省略号 */
  const wrapText = (ctx, text, maxWidth, maxLines) => {
    const chars = Array.from(String(text || '').replace(/\s+/g, ' ').trim())
    const lines = []
    let current = ''
    for (const ch of chars) {
      const test = current + ch
      if (ctx.measureText(test).width > maxWidth && current) {
        lines.push(current)
        current = ch
        if (lines.length === maxLines) break
      } else {
        current = test
      }
    }
    if (lines.length < maxLines && current) lines.push(current)
    if (lines.length === maxLines) {
      let last = lines[maxLines - 1]
      const rest = chars.slice(lines.join('').length)
      if (rest.length > 0) {
        while (ctx.measureText(last + '…').width > maxWidth && last.length > 1) {
          last = last.slice(0, -1)
        }
        lines[maxLines - 1] = last + '…'
      }
    }
    return lines
  }

  const loadImage = (src) => new Promise((resolve) => {
    if (!src) { resolve(null); return }
    const img = new Image()
    // 只有跨域图片才需要 crossOrigin。同源图片（例如本站 /uploads/xxx.jpg）设了反而会
    // 触发一次 CORS 校验，服务端没回 ACAO 时图片会直接加载失败，海报里就只剩渐变底色。
    try {
      const abs = new URL(src, window.location.href)
      if (abs.origin !== window.location.origin) img.crossOrigin = 'anonymous'
    } catch (e) {
      img.crossOrigin = 'anonymous'
    }
    img.onload = () => resolve(img)
    img.onerror = () => resolve(null)
    img.src = src
  })

  /**
   * 海报封面加载：优先直连；如果图源不支持跨域（canvas 会被污染，导出会失败），
   * 就走后端 /file/upload-from-url 把图抓成自己站内的副本再画，保证海报一定能导出。
   */
  const loadPosterCover = async (src) => {
    if (!src) return null
    try {
      const abs = new URL(src, window.location.href)
      // 站外图源统一走本站代理接口，CORS 头由我们自己控制，
      // 不再依赖 picsum / 各家 CDN 在不同网络下是否返回 ACAO（游客也可用）
      if (abs.origin !== window.location.origin) {
        const proxied = await loadImage(posterCoverProxy(src))
        if (proxied) return proxied
      }
    } catch (e) {
      console.warn('[poster] 封面代理失败，回退直连', e)
    }
    return await loadImage(src)
  }

  const makeModernPoster = async (template) => {
    if (posterBuilding.value) return
    posterBuilding.value = true
    try {
      const canvas = document.createElement('canvas')
      canvas.width = POSTER_W
      canvas.height = POSTER_H
      const ctx = canvas.getContext('2d')
      const coverSrc = imageList.value.find(u => !isVideo(u)) || ''
      const cover = await loadPosterCover(coverSrc)
      const title = detail.value.title || '未命名灵感'
      const plain = String(detail.value.content || DEFAULT_DESC).replace(/\s+/g, ' ')
      const authorName = detail.value.nickname || '灵感创作者'
      const tag = tagList.value[0] || '灵感'
      const shareUrl = `${window.location.origin}/#/detail/${detail.value.id}`
      const { default: QRCode } = await import('qrcode')
      const qrDataUrl = await QRCode.toDataURL(shareUrl, {
        width: 320,
        margin: 0,
        color: { dark: '#111715', light: '#ffffff' }
      })
      const qrImg = await loadImage(qrDataUrl)

      const drawQr = (x, y, size) => {
        if (!qrImg) return
        ctx.fillStyle = '#ffffff'
        roundRectPath(ctx, x - 12, y - 12, size + 24, size + 24, 14)
        ctx.fill()
        ctx.drawImage(qrImg, x, y, size, size)
      }

      if (template === 'minimal') {
        ctx.fillStyle = '#ffffff'
        ctx.fillRect(0, 0, POSTER_W, POSTER_H)
        ctx.fillStyle = '#a0a6a2'
        ctx.font = '500 24px "PingFang SC","Hiragino Sans GB","Microsoft YaHei",sans-serif'
        ctx.fillText('LINGGANGAN · INSPIRE DAILY', 80, 100)

        ctx.save()
        roundRectPath(ctx, 80, 160, 920, 650, 8)
        ctx.clip()
        if (cover) drawImageCover(ctx, cover, 80, 160, 920, 650)
        else { ctx.fillStyle = '#e7ecea'; ctx.fillRect(80, 160, 920, 650) }
        ctx.restore()

        ctx.fillStyle = '#202522'
        ctx.font = '500 58px "PingFang SC","Hiragino Sans GB","Microsoft YaHei",sans-serif'
        let cursorY = 905
        wrapText(ctx, title, 920, 2).forEach((line, i) => ctx.fillText(line, 80, cursorY + i * 72))
        cursorY += 150

        ctx.fillStyle = '#8d9490'
        ctx.font = '400 25px "PingFang SC","Hiragino Sans GB","Microsoft YaHei",sans-serif'
        ctx.fillText(`${tag} · ${authorName} · ${publishText.value}`, 80, cursorY)
        cursorY += 58
        ctx.fillStyle = '#555e5a'
        ctx.font = '400 29px "PingFang SC","Hiragino Sans GB","Microsoft YaHei",sans-serif'
        wrapText(ctx, plain, 920, 2).forEach((line, i) => ctx.fillText(line, 80, cursorY + i * 46))

        ctx.strokeStyle = '#e8ecea'
        ctx.lineWidth = 2
        ctx.beginPath()
        ctx.moveTo(80, 1270)
        ctx.lineTo(1000, 1270)
        ctx.stroke()
        ctx.fillStyle = '#9aa19d'
        ctx.font = '400 23px "PingFang SC","Hiragino Sans GB","Microsoft YaHei",sans-serif'
        ctx.fillText('扫码查看完整灵感', 80, 1340)
        drawQr(875, 1220, 150)
      } else {
        if (cover) drawImageCover(ctx, cover, 0, 0, POSTER_W, POSTER_H)
        else { ctx.fillStyle = '#27322f'; ctx.fillRect(0, 0, POSTER_W, POSTER_H) }
        const shade = ctx.createLinearGradient(0, POSTER_H * .35, 0, POSTER_H)
        shade.addColorStop(0, 'rgba(4,8,7,0)')
        shade.addColorStop(1, 'rgba(4,8,7,.88)')
        ctx.fillStyle = shade
        ctx.fillRect(0, 0, POSTER_W, POSTER_H)

        ctx.fillStyle = '#ffffff'
        ctx.font = '700 25px "PingFang SC","Hiragino Sans GB","Microsoft YaHei",sans-serif'
        ctx.fillText('INSPIRE DAILY', 70, 95)
        ctx.textAlign = 'right'
        ctx.fillText('ISSUE 09', POSTER_W - 70, 95)
        ctx.textAlign = 'left'

        ctx.fillStyle = '#f3c56b'
        ctx.font = '800 22px "PingFang SC","Hiragino Sans GB","Microsoft YaHei",sans-serif'
        const tagW = ctx.measureText(tag.toUpperCase()).width + 40
        ctx.fillRect(70, 840, Math.max(tagW, 150), 46)
        ctx.fillStyle = '#261d0d'
        ctx.fillText(tag.toUpperCase(), 88, 872)

        ctx.fillStyle = '#ffffff'
        ctx.font = '800 60px "PingFang SC","Hiragino Sans GB","Microsoft YaHei",sans-serif'
        let cursorY = 960
        wrapText(ctx, title, 880, 2).forEach((line, i) => ctx.fillText(line, 70, cursorY + i * 76))
        cursorY += 165
        ctx.fillStyle = 'rgba(255,255,255,.82)'
        ctx.font = '400 29px "PingFang SC","Hiragino Sans GB","Microsoft YaHei",sans-serif'
        wrapText(ctx, plain, 880, 2).forEach((line, i) => ctx.fillText(line, 70, cursorY + i * 46))

        ctx.strokeStyle = 'rgba(255,255,255,.32)'
        ctx.beginPath()
        ctx.moveTo(70, 1265)
        ctx.lineTo(1010, 1265)
        ctx.stroke()
        ctx.fillStyle = 'rgba(255,255,255,.88)'
        ctx.font = '500 25px "PingFang SC","Hiragino Sans GB","Microsoft YaHei",sans-serif'
        ctx.fillText(authorName, 70, 1340)
        drawQr(875, 1215, 150)
      }

      posterUrl.value = canvas.toDataURL('image/png')
      posterVisible.value = true
      posterTemplatePickerVisible.value = false
      showSharePanel.value = false
    } catch (e) {
      console.error('[poster]', e)
      ElMessage.error('海报生成失败，请稍后再试')
    } finally {
      posterBuilding.value = false
    }
  }

  const makePoster = async (template = 'paper') => {
    if (template !== 'paper') {
      await makeModernPoster(template)
      return
    }
    if (posterBuilding.value) return
    posterBuilding.value = true
    try {
      const canvas = document.createElement('canvas')
      canvas.width = POSTER_W
      canvas.height = POSTER_H
      const ctx = canvas.getContext('2d')

      // 纸张底色 + 细边框
      const bg = ctx.createLinearGradient(0, 0, POSTER_W, POSTER_H)
      bg.addColorStop(0, '#fdfaf5')
      bg.addColorStop(1, '#eef5ea')
      ctx.fillStyle = bg
      ctx.fillRect(0, 0, POSTER_W, POSTER_H)
      ctx.strokeStyle = 'rgba(79,138,72,.35)'
      ctx.lineWidth = 3
      roundRectPath(ctx, 32, 32, POSTER_W - 64, POSTER_H - 64, 36)
      ctx.stroke()

      const pad = 84
      const contentW = POSTER_W - pad * 2

      // 顶部标识
      ctx.fillStyle = '#4f8a48'
      ctx.font = '600 30px "PingFang SC","Hiragino Sans GB","Microsoft YaHei",sans-serif'
      ctx.fillText('灵感手账 · INSPIRE DAILY', pad, 140)

      // 主图
      const imgY = 180
      const imgH = 560
      // 海报封面优先取图片，避免视频首帧取不到
      const coverSrc = imageList.value.find(u => !isVideo(u)) || ''
      const cover = await loadPosterCover(coverSrc)
      ctx.save()
      roundRectPath(ctx, pad, imgY, contentW, imgH, 32)
      ctx.clip()
      if (cover) {
        drawImageCover(ctx, cover, pad, imgY, contentW, imgH)
      } else {
        const g = ctx.createLinearGradient(pad, imgY, pad + contentW, imgY + imgH)
        g.addColorStop(0, '#cfe4c8')
        g.addColorStop(1, '#f3ddc4')
        ctx.fillStyle = g
        ctx.fillRect(pad, imgY, contentW, imgH)
      }
      ctx.restore()

      // 标签
      let cursorY = imgY + imgH + 76
      const tag = tagList.value[0]
      if (tag) {
        ctx.font = '600 26px "PingFang SC","Hiragino Sans GB","Microsoft YaHei",sans-serif'
        const tagW = ctx.measureText('#' + tag).width + 44
        ctx.fillStyle = '#e6f2e4'
        roundRectPath(ctx, pad, cursorY - 34, tagW, 52, 26)
        ctx.fill()
        ctx.fillStyle = '#4f8a48'
        ctx.fillText('#' + tag, pad + 22, cursorY + 2)
      }

      // 标题
      cursorY += 96
      ctx.fillStyle = '#3f362c'
      ctx.font = '700 54px "PingFang SC","Hiragino Sans GB","Microsoft YaHei",sans-serif'
      const titleLines = wrapText(ctx, detail.value.title || '未命名灵感', contentW, 2)
      titleLines.forEach((line, i) => ctx.fillText(line, pad, cursorY + i * 72))
      cursorY += titleLines.length * 72 + 26

      // 正文摘要
      ctx.fillStyle = '#6b5c4c'
      ctx.font = '400 30px "PingFang SC","Hiragino Sans GB","Microsoft YaHei",sans-serif'
      const plain = String(detail.value.content || DEFAULT_DESC).replace(/\s+/g, ' ')
      const descLines = wrapText(ctx, plain, contentW, 2)
      descLines.forEach((line, i) => ctx.fillText(line, pad, cursorY + i * 46))

      // 底部：作者 + 二维码
      const footY = POSTER_H - 250
      ctx.strokeStyle = 'rgba(79,138,72,.28)'
      ctx.lineWidth = 2
      ctx.beginPath()
      ctx.moveTo(pad, footY - 50)
      ctx.lineTo(POSTER_W - pad, footY - 50)
      ctx.stroke()

      const authorName = detail.value.nickname || '灵感创作者'
      ctx.fillStyle = '#3f362c'
      ctx.font = '600 34px "PingFang SC","Hiragino Sans GB","Microsoft YaHei",sans-serif'
      ctx.fillText(authorName, pad, footY + 20)
      ctx.fillStyle = '#93826f'
      ctx.font = '400 26px "PingFang SC","Hiragino Sans GB","Microsoft YaHei",sans-serif'
      ctx.fillText(publishText.value, pad, footY + 68)

      // 二维码
      const shareUrl = `${window.location.origin}/#/detail/${detail.value.id}`
      const { default: QRCode } = await import('qrcode')
      const qrDataUrl = await QRCode.toDataURL(shareUrl, {
        width: 300,
        margin: 0,
        color: { dark: '#3f362c', light: '#ffffff' }
      })
      const qrImg = await loadImage(qrDataUrl)
      const qrSize = 180
      const qrX = POSTER_W - pad - qrSize
      const qrY = footY - 30
      if (qrImg) {
        ctx.fillStyle = '#ffffff'
        roundRectPath(ctx, qrX - 14, qrY - 14, qrSize + 28, qrSize + 28, 18)
        ctx.fill()
        ctx.drawImage(qrImg, qrX, qrY, qrSize, qrSize)
      }
      ctx.fillStyle = '#93826f'
      ctx.font = '400 24px "PingFang SC","Hiragino Sans GB","Microsoft YaHei",sans-serif'
      ctx.textAlign = 'center'
      ctx.fillText('扫码查看灵感', qrX + qrSize / 2, qrY + qrSize + 46)
      ctx.textAlign = 'left'

      try {
        posterUrl.value = canvas.toDataURL('image/png')
      } catch (err) {
        // 画布被跨域图片污染时无法导出，给出明确提示而不是静默失败
        console.error('[poster] 导出失败', err)
        ElMessage.error('这张封面图不支持导出，已跳过图片，请重试')
        return
      }
      posterVisible.value = true
      posterTemplatePickerVisible.value = false
      showSharePanel.value = false
    } catch (e) {
      ElMessage.error('海报生成失败，请稍后再试')
      console.error('[poster]', e)
    } finally {
      posterBuilding.value = false
    }
  }

  return {
    posterVisible,
    posterUrl,
    posterBuilding,
    posterTemplatePickerVisible,
    makePoster
  }
}
