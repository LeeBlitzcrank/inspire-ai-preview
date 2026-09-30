/**
 * 文件：frontend/src/pages/create/composables/useCreateVoiceInput.js
 * 所属模块：用户端页面和交互流程
 * 主要职责：Vue Composable，集中管理页面状态、异步流程和生命周期
 * 维护说明：注释解释文件边界和核心意图，具体业务规则以方法、组件和主文档说明为准。
 * INSPIRE_FILE_HEADER
 */
import {onUnmounted, ref} from 'vue'

export function useCreateVoiceInput({form, descRef, setContent, onDescInput}) {
  // 语音识别
  const voiceActive = ref(false)
  const voiceError = ref('')
  let recognition = null

  const initRecognition = () => {
    const SR = window.SpeechRecognition || window.webkitSpeechRecognition
    if (!SR) { voiceError.value = '当前浏览器不支持语音识别（推荐使用 Chrome/Safari）'; return null }
    const r = new SR()
    r.lang = 'zh-CN'
    r.interimResults = true
    r.continuous = true
    r.maxAlternatives = 1
    r.onresult = (e) => {
      let transcript = ''
      for (let i = e.resultIndex; i < e.results.length; i++) {
        transcript += e.results[i][0].transcript
      }
      // 语音结果插入富文本编辑器光标处
      if (descRef.value) {
        descRef.value.focus()
        document.execCommand('insertText', false, transcript)
        onDescInput()
      }
    }
    r.onerror = (e) => {
      voiceError.value = '语音识别错误: ' + e.error
      voiceActive.value = false
    }
    r.onend = () => {
      if (voiceActive.value) {
        try { r.start() } catch(e) {}
      }
    }
    return r
  }

  const toggleVoice = () => {
    voiceError.value = ''
    if (voiceActive.value) {
      if (recognition) { recognition.stop(); recognition = null }
      voiceActive.value = false
      return
    }
    const r = initRecognition()
    if (!r) return
    recognition = r
    try {
      r.start()
      voiceActive.value = true
    } catch (e) {
      voiceError.value = '启动失败，请检查麦克风权限'
    }
  }

  onUnmounted(() => {
    if (recognition) {
      recognition.abort()
      recognition = null
    }
  })

  return {voiceActive, voiceError, toggleVoice}
}
