<!--
  文件：frontend/src/pages/Forbidden.vue
  所属模块：用户端页面和交互流程
  主要职责：Vue 页面或组件，负责界面渲染、交互事件和页面状态衔接
  维护说明：注释解释文件边界和核心意图，具体业务规则以方法、组件和主文档说明为准。
  INSPIRE_FILE_HEADER
-->
<template>
  <div class="err-page"><div class="err-box">
    <div class="err-code">403</div>
    <div class="err-text">无权访问</div>
    <p class="err-desc">你没有权限访问这个页面</p>
    <el-button type="primary" @click="$router.back()">返回上一页</el-button>
    <el-button @click="$router.push('/')">回到首页</el-button>
  </div></div>
</template>
<style scoped>
.err-page { height:100vh; display:flex; align-items:center; justify-content:center; background:#fbfcfe; }
.err-box { text-align:center; }
.err-code { font-size:80px; font-weight:700; color:#e6a23c; line-height:1; margin-bottom:16px; }
.err-text { font-size:24px; color:#1d1d1f; margin-bottom:8px; }
.err-desc { color:#86868b; margin-bottom:32px; }
</style>
