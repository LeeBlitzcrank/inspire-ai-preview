import {onCLS, onFCP, onINP, onLCP, onTTFB} from 'web-vitals'

const API_BASE = import.meta.env.VITE_API_BASE
  ? `${import.meta.env.VITE_API_BASE}/api`
  : '/api'
const SAMPLE_RATE = Number(import.meta.env.VITE_WEB_VITALS_SAMPLE_RATE ?? 0.2)
const sampled = Math.random() < Math.max(0, Math.min(1, SAMPLE_RATE))

const deviceType = () => {
  if (window.matchMedia('(max-width: 768px)').matches) return 'mobile'
  if (window.matchMedia('(max-width: 1200px)').matches) return 'tablet'
  return 'desktop'
}

const browserName = () => {
  const ua = navigator.userAgent
  if (/Edg\//.test(ua)) return 'Edge'
  if (/Chrome\//.test(ua)) return 'Chrome'
  if (/Safari\//.test(ua) && !/Chrome\//.test(ua)) return 'Safari'
  if (/Firefox\//.test(ua)) return 'Firefox'
  return 'Other'
}

const report = (metric) => {
  if (!sampled || sessionStorage.getItem('__disable_web_vitals__') === '1') return

  const payload = {
    name: metric.name,
    value: metric.value,
    rating: metric.rating,
    delta: metric.delta,
    id: metric.id,
    navigationType: metric.navigationType,
    path: `${window.location.pathname}${window.location.hash || ''}`,
    device: deviceType(),
    browser: browserName(),
    appVersion: import.meta.env.VITE_APP_VERSION || '1.0.0'
  }
  const body = JSON.stringify(payload)
  const url = `${API_BASE}/inspire/public/vitals`

  if (navigator.sendBeacon) {
    const ok = navigator.sendBeacon(url, new Blob([body], { type: 'application/json' }))
    if (ok) return
  }
  fetch(url, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body,
    keepalive: true
  }).catch(() => {})
}

export const initWebVitals = () => {
  onFCP(report)
  onLCP(report)
  onCLS(report)
  onINP(report)
  onTTFB(report)
}
