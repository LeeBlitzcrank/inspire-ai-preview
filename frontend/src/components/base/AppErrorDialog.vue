<template>
  <Teleport to="body">
    <div v-if="state.visible" class="app-error-mask" role="presentation">
      <section class="app-error-dialog" role="alertdialog" aria-modal="true" aria-labelledby="app-error-title">
        <div class="app-error-icon" aria-hidden="true">!</div>
        <h3 id="app-error-title">{{ state.title }}</h3>
        <p>{{ state.message }}</p>
        <pre v-if="state.detail">{{ state.detail }}</pre>
        <button type="button" autofocus @click="close">关闭</button>
      </section>
    </div>
  </Teleport>
</template>

<script setup>
import { errorDialogState as state, closeErrorDialog as close } from '@/utils/errorDialog.js'
</script>

<style scoped>
.app-error-mask {
  position: fixed;
  inset: 0;
  z-index: 12000;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
  background: rgba(17, 24, 39, .42);
  backdrop-filter: blur(3px);
}
.app-error-dialog {
  width: min(320px, 100%);
  padding: 24px 20px 18px;
  border: 1px solid #f1d6d1;
  border-radius: 18px;
  background: #fff;
  box-shadow: 0 24px 70px rgba(35, 24, 22, .24);
  text-align: center;
}
.app-error-icon {
  width: 42px;
  height: 42px;
  margin: 0 auto 12px;
  border-radius: 50%;
  background: #fff1ef;
  color: #c83f32;
  font-size: 24px;
  font-weight: 800;
  line-height: 42px;
}
h3 { margin: 0 0 8px; color: #2f2523; font-size: 17px; }
p { margin: 0; color: #6f5b56; font-size: 14px; line-height: 1.65; white-space: pre-wrap; }
pre {
  max-height: 120px;
  overflow: auto;
  margin: 12px 0 0;
  padding: 8px;
  border-radius: 8px;
  background: #faf7f6;
  color: #8a6e68;
  font-size: 11px;
  text-align: left;
  white-space: pre-wrap;
}
button {
  width: 100%;
  height: 42px;
  margin-top: 18px;
  border: none;
  border-radius: 11px;
  background: #b84a3d;
  color: #fff;
  font: inherit;
  font-size: 14px;
  font-weight: 700;
  cursor: pointer;
}
button:active { transform: scale(.985); }
</style>
