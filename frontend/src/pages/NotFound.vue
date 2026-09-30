<!--
  文件：frontend/src/pages/NotFound.vue
  所属模块：用户端页面和交互流程
  主要职责：Vue 页面或组件，负责界面渲染、交互事件和页面状态衔接
  维护说明：注释解释文件边界和核心意图，具体业务规则以方法、组件和主文档说明为准。
  INSPIRE_FILE_HEADER
-->
<template>
  <div class="nf-page">
    <div class="nf-box">
      <div class="nf-code">404</div>
      <div class="nf-text">页面不存在</div>
      <p class="nf-desc">你访问的页面已经消失～</p>
      <el-button type="primary" @click="$router.push('/')">返回首页</el-button>
    </div>
  </div>
</template>
<style scoped>
.nf-page { height:100vh; display:flex; align-items:center; justify-content:center; background:#fbfcfe; }
.nf-box { text-align:center; }
.nf-code { font-size:80px; font-weight:700; color:#409eff; line-height:1; margin-bottom:16px; }
.nf-text { font-size:24px; color:#1d1d1f; margin-bottom:8px; }
.nf-desc { color:#86868b; margin-bottom:32px; }
</style>
