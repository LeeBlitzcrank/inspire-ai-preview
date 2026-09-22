<template>
  <div class="device-shell" :class="{ 'is-bare': !enabled }">
    <div ref="stageRef" class="device-stage">
    <div ref="frameRef" class="device-frame">
      <div v-if="enabled" class="device-statusbar">
        <span class="status-time">9:41</span>
        <div class="status-icons">
          <svg viewBox="0 0 20 12" aria-hidden="true">
            <rect x="0" y="8" width="3" height="4" rx="1" fill="currentColor"/>
            <rect x="4.5" y="6" width="3" height="6" rx="1" fill="currentColor"/>
            <rect x="9" y="3.5" width="3" height="8.5" rx="1" fill="currentColor"/>
            <rect x="13.5" y="0.5" width="3" height="11.5" rx="1" fill="currentColor"/>
          </svg>
          <span class="signal-5g">5G</span>
          <svg viewBox="0 0 18 13" aria-hidden="true">
            <path d="M1 4.2C5.5 0 12.5 0 17 4.2" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round"/>
            <path d="M4 7.2c2.8-2.5 7.2-2.5 10 0" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round"/>
            <path d="M7 10c1.2-1 2.8-1 4 0" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round"/>
            <circle cx="9" cy="12" r="1.1" fill="currentColor"/>
          </svg>
          <svg viewBox="0 0 26 12" aria-hidden="true">
            <rect x="0.8" y="0.8" width="21" height="10.4" rx="3" fill="none" stroke="currentColor" stroke-width="1.4" opacity=".55"/>
            <rect x="2.6" y="2.6" width="16.2" height="6.8" rx="1.8" fill="currentColor"/>
            <path d="M23.5 4v4c1.1-.3 1.8-1 1.8-2s-.7-1.7-1.8-2z" fill="currentColor" opacity=".55"/>
          </svg>
        </div>
      </div>
      <div ref="viewportRef" class="device-viewport">
        <div ref="canvasRef" class="device-canvas">
          <slot />
        </div>
      </div>
    </div>
    </div>
  </div>
</template>

<script setup>
import { nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'

const props = defineProps({
  enabled: { type: Boolean, default: true }
})

const LOGICAL_WIDTH = 390
const LOGICAL_HEIGHT = 844
const FRAME_WIDTH = 412
const FRAME_HEIGHT = 866
const OUTER_GUTTER = 24
const viewportRef = ref(null)
const canvasRef = ref(null)
const stageRef = ref(null)
const frameRef = ref(null)
let resizeObserver = null

function fitLayout() {
  const stage = stageRef.value
  const frame = frameRef.value
  const viewport = viewportRef.value
  const canvas = canvasRef.value
  if (!stage || !frame || !viewport || !canvas) return

  if (!props.enabled) {
    stage.style.width = ''
    stage.style.height = ''
    frame.style.transform = ''
    return
  }

  const availableWidth = (window.visualViewport?.width || window.innerWidth) - OUTER_GUTTER
  const availableHeight = (window.visualViewport?.height || window.innerHeight) - OUTER_GUTTER
  const frameScale = Math.min(
    availableWidth / FRAME_WIDTH,
    availableHeight / FRAME_HEIGHT,
    1
  )
  stage.style.width = `${FRAME_WIDTH * frameScale}px`
  stage.style.height = `${FRAME_HEIGHT * frameScale}px`
  frame.style.transform = `scale(${frameScale})`

  const width = viewport.clientWidth
  const height = viewport.clientHeight
  const scale = Math.min(width / LOGICAL_WIDTH, height / LOGICAL_HEIGHT)
  const left = (width - LOGICAL_WIDTH * scale) / 2
  const top = (height - LOGICAL_HEIGHT * scale) / 2
  canvas.style.transform = `translate(${left}px, ${top}px) scale(${scale})`
}

onMounted(async () => {
  await nextTick()
  fitLayout()
  if (window.ResizeObserver && stageRef.value) {
    resizeObserver = new ResizeObserver(fitLayout)
    resizeObserver.observe(stageRef.value)
  }
  window.addEventListener('resize', fitLayout)
  window.visualViewport?.addEventListener('resize', fitLayout)
})

watch(() => props.enabled, async () => {
  await nextTick()
  fitLayout()
})

onBeforeUnmount(() => {
  if (resizeObserver) resizeObserver.disconnect()
  window.removeEventListener('resize', fitLayout)
  window.visualViewport?.removeEventListener('resize', fitLayout)
})
</script>

<style>
/* 用户端页面统一手机壳；后台通过 enabled=false 使用桌面全宽布局。 */
.device-shell {
  min-height: 100dvh;
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 12px;
  overflow: hidden;
  background:
    radial-gradient(circle at 12% 8%, rgba(50,124,240,.13), transparent 30%),
    radial-gradient(circle at 88% 18%, rgba(30,170,122,.11), transparent 28%),
    #e9eef4;
}
.device-stage {
  position: relative;
  flex: 0 0 auto;
}
.device-frame {
  --screen-w: 390px;
  --screen-h: 844px;
  --status-h: 32px;
  position: relative;
  width: calc(var(--screen-w) + 22px);
  height: calc(var(--screen-h) + 22px);
  padding: 11px;
  border-radius: 46px;
  background: #10161d;
  box-shadow:
    0 30px 70px rgba(25, 42, 64, .24),
    inset 0 0 0 1px rgba(255,255,255,.08);
  transform-origin: top left;
}
.device-statusbar {
  position: absolute;
  z-index: 3;
  top: 11px;
  left: 11px;
  right: 11px;
  height: var(--status-h);
  padding: 0 17px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-radius: 35px 35px 0 0;
  background: #fff;
  color: #121820;
  font-size: 11px;
  font-weight: 800;
  line-height: 1;
}
.status-time { letter-spacing: .01em; }
.status-icons { display: flex; align-items: center; gap: 5px; }
.status-icons svg { display: block; width: auto; height: 11px; }
.signal-5g { font-size: 9px; font-weight: 850; }

.device-viewport {
  position: absolute;
  top: calc(11px + var(--status-h));
  left: 11px;
  right: 11px;
  bottom: 11px;
  overflow: hidden;
  border-radius: 0 0 35px 35px;
  background: #f3f6fa;
}
.device-canvas {
  position: absolute;
  top: 0;
  left: 0;
  width: 390px;
  height: 844px;
  transform-origin: top left;
  background: #fff;
}

/* app 原有页面在固定画布内滚动，不参与设备尺寸响应式重排。 */
.device-canvas .app-design,
.device-canvas .app-page {
  width: 390px;
  height: 844px;
  min-height: 844px;
}
.device-canvas .app-page {
  min-width: 0;
  overflow-x: hidden;
  overflow-y: auto;
  overscroll-behavior-y: contain;
  -webkit-overflow-scrolling: touch;
  transform: none;
  transition: none;
}
.device-canvas .app-page > * {
  min-height: 844px;
}
.device-canvas .page-enter-active,
.device-canvas .page-leave-active {
  transition: opacity .16s ease;
}
.device-canvas .page-enter-from,
.device-canvas .page-leave-to {
  transform: none;
}

/* 后台管理不套手机框。 */
.device-shell.is-bare {
  display: block;
  min-height: 100dvh;
  padding: 0;
  background: #f5f7fa;
}
.is-bare .device-frame {
  width: 100%;
  height: auto;
  min-height: 100dvh;
  padding: 0;
  border-radius: 0;
  background: transparent;
  box-shadow: none;
  transform: none !important;
}
.is-bare .device-stage {
  width: 100% !important;
  height: auto !important;
}
.is-bare .device-statusbar { display: none; }
.is-bare .device-viewport,
.is-bare .device-canvas {
  position: static;
  width: 100%;
  height: auto;
  min-height: 100dvh;
  overflow: visible;
  transform: none !important;
}
.is-bare .device-canvas .app-design,
.is-bare .device-canvas .app-page {
  width: 100%;
  height: auto;
  min-height: 100dvh;
}
.is-bare .device-canvas .app-page {
  overflow: visible;
}

</style>
