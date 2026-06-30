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
