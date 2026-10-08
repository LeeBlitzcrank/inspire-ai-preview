/**
 * 文件：frontend/src/utils/validation.js
 * 所属模块：前端通用工具和基础能力
 * 主要职责：提供与后端一致的输入长度、格式和密码规则提示
 * 维护说明：这里是前端提前校验，后端仍保留最终校验；修改规则时必须同步 ValidationConstants.java。
 * INSPIRE_FILE_HEADER
 */

export const INPUT_LIMITS = Object.freeze({
  usernameMin: 4,
  usernameMax: 20,
  nicknameMin: 2,
  nicknameMax: 20,
  passwordMin: 8,
  passwordMax: 64,
  emailMax: 254,
  titleMax: 16,
  contentMax: 20000,
  commentMax: 500,
  messageMax: 1000,
  aiKeywordMax: 100,
  worldSourceTitleMax: 120,
  worldSourceAuthorMax: 80,
  worldSourceTextMax: 6000,
  worldGuidanceMax: 500,
  worldChoiceTextMax: 160
})

const USERNAME_PATTERN = /^[A-Za-z0-9_]{4,20}$/
const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
const WEAK_PASSWORDS = new Set([
  '12345678', '123456789', 'password', 'password1', 'qwerty123',
  '11111111', '00000000', 'abc12345', 'admin123', 'iloveyou',
  'letmein', 'welcome1', '88888888', '66666666'
])

export function textLength(value) {
  return Array.from(String(value || '')).length
}

export function validateUsername(value) {
  const username = String(value || '').trim()
  if (!USERNAME_PATTERN.test(username)) return '账号需为4-20位字母、数字或下划线'
  return ''
}

export function validateEmail(value) {
  const email = String(value || '').trim()
  if (!email) return '请输入邮箱'
  if (textLength(email) > INPUT_LIMITS.emailMax || !EMAIL_PATTERN.test(email)) return '邮箱格式或长度不正确'
  return ''
}

export function validatePassword(value, { username = '', email = '' } = {}) {
  const password = String(value || '')
  const length = textLength(password)
  if (length < INPUT_LIMITS.passwordMin || length > INPUT_LIMITS.passwordMax) {
    return '密码长度需为8-64位'
  }
  if (/^\d+$/.test(password)) return '密码不能全部由数字组成'
  if (WEAK_PASSWORDS.has(password.toLowerCase())) return '密码过于简单，请更换更安全的密码'
  const account = String(username || '').trim().toLowerCase()
  if (account.length >= 3 && password.toLowerCase().includes(account)) return '密码不能包含账号'
  const localPart = String(email || '').split('@')[0].toLowerCase()
  if (localPart.length >= 3 && password.toLowerCase().includes(localPart)) return '密码不能包含邮箱前缀'
  return ''
}

export function validateNickname(value) {
  const nickname = String(value || '').trim().replace(/\s+/g, ' ')
  const length = textLength(nickname)
  if (length < INPUT_LIMITS.nicknameMin || length > INPUT_LIMITS.nicknameMax) {
    return '昵称长度需为2-20个字符'
  }
  return ''
}

export function maxLengthMessage(label, max) {
  return `${label}不能超过${max}个字符`
}

export function smsCooldownFromError(error, fallback = 60) {
  const message = error?.response?.data?.msg
    || error?.data?.msg
    || error?.msg
    || error?.message
    || ''
  const match = String(message).match(/(\d+)\s*秒/)
  if (match) {
    const seconds = Number(match[1])
    return Number.isFinite(seconds) && seconds > 0 ? Math.ceil(seconds) : 0
  }
  return Number(error?.response?.status) === 429 ? fallback : 0
}

const SMS_COOLDOWN_PREFIX = 'inspire:sms-cooldown:'

const smsCooldownKey = (scope) => `${SMS_COOLDOWN_PREFIX}${scope}`

export function saveSmsCooldown(scope, phone, seconds) {
  const until = Date.now() + Math.max(1, Number(seconds) || 60) * 1000
  localStorage.setItem(smsCooldownKey(scope), JSON.stringify({
    phone: String(phone || '').trim(),
    until
  }))
}

export function getSmsCooldown(scope, phone = '') {
  try {
    const raw = localStorage.getItem(smsCooldownKey(scope))
    if (!raw) return null
    const state = JSON.parse(raw)
    const remainingSeconds = Math.ceil((Number(state.until) - Date.now()) / 1000)
    const expectedPhone = String(phone || '').trim()
    if (remainingSeconds <= 0 || (expectedPhone && state.phone !== expectedPhone)) {
      if (remainingSeconds <= 0) localStorage.removeItem(smsCooldownKey(scope))
      return null
    }
    return {
      phone: state.phone,
      remainingSeconds
    }
  } catch {
    localStorage.removeItem(smsCooldownKey(scope))
    return null
  }
}

export function clearSmsCooldown(scope) {
  localStorage.removeItem(smsCooldownKey(scope))
}
