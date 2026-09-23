<template>
  <div class="series-page">
    <header class="series-top">
      <button type="button" aria-label="返回" @click="goBack">‹</button>
      <b>系列目录</b>
      <span></span>
    </header>
    <AppState :state="state" :rows="5" error-text="系列加载失败" @retry="load">
      <section v-if="series.id" class="series-hero">
        <img v-if="series.cover" :src="thumbOf(series.cover, 800)" alt="">
        <div class="hero-shade"></div>
        <div class="hero-copy">
          <span>INSPIRE SERIES</span>
          <h1>{{ series.name }}</h1>
          <p>{{ series.description || '一组适合连续阅读的灵感记录。' }}</p>
          <small>共 {{ series.total }} 篇</small>
        </div>
      </section>

      <section v-if="series.id" class="article-list">
        <button v-for="(item, index) in series.articles" :key="item.id" type="button"
                @click="router.push(`/detail/${item.id}`)">
          <span class="order">{{ String(index + 1).padStart(2, '0') }}</span>
          <span class="article-copy">
            <b>{{ item.title }}</b>
            <small>{{ item.tag }} · {{ item.publishCity || '灵感手账' }}</small>
          </span>
          <span class="arrow">›</span>
        </button>
      </section>
    </AppState>
  </div>
</template>

<script setup>
import {computed, ref, watch} from 'vue'
import {useRoute, useRouter} from 'vue-router'
import {getSeriesDetail} from '@/api/inspire.js'
import {thumbOf} from '@/utils/media.js'

const route = useRoute()
const router = useRouter()
const series = ref({})
const loading = ref(false)
const error = ref('')
const state = computed(() => {
  if (loading.value && !series.value.id) return 'loading'
  if (error.value && !series.value.id) return 'error'
  return series.value.id ? 'ready' : 'empty'
})

const goBack = () => {
  if (window.history.state?.back) {
    router.back()
  } else {
    router.replace('/')
  }
}

const load = async () => {
  loading.value = true
  error.value = ''
  try {
    const res = await getSeriesDetail(route.params.id)
    if (res.code !== 200) throw new Error(res.msg || '系列不存在')
    series.value = res.data || {}
  } catch (e) {
    error.value = e?.response?.data?.msg || e?.message || '系列加载失败'
  } finally {
    loading.value = false
  }
}

watch(() => route.params.id, load, { immediate: true })
</script>

<style scoped>
.series-page { min-height:100vh; background:#f7fbfa; padding-bottom:40px; }
.series-top { position:sticky; top:0; z-index:5; display:grid; grid-template-columns:40px 1fr 40px; align-items:center; padding:12px 14px; background:rgba(255,255,255,.95); backdrop-filter:blur(8px); }
.series-top button { width:36px; height:36px; border:0; border-radius:50%; background:#fff; font-size:25px; color:#315f5a; cursor:pointer; box-shadow:0 2px 10px rgba(32,71,66,.08); }
.series-top b { text-align:center; font-size:16px; color:#203b37; }
.series-hero { position:relative; height:270px; margin:8px 14px 16px; overflow:hidden; border-radius:22px; background:#dfece9; }
.series-hero img { width:100%; height:100%; object-fit:cover; display:block; }
.hero-shade { position:absolute; inset:0; background:linear-gradient(180deg,rgba(8,24,20,.04),rgba(8,24,20,.82)); }
.hero-copy { position:absolute; left:20px; right:20px; bottom:18px; color:#fff; }
.hero-copy > span { font-size:9px; letter-spacing:.14em; opacity:.72; }
.hero-copy h1 { margin:7px 0 6px; font-size:23px; line-height:1.25; }
.hero-copy p { margin:0; font-size:12px; line-height:1.6; opacity:.86; }
.hero-copy small { display:block; margin-top:9px; font-size:10.5px; opacity:.72; }
.article-list { margin:0 14px; overflow:hidden; border:1px solid #e1eeeb; border-radius:18px; background:#fff; }
.article-list button { width:100%; display:flex; align-items:center; gap:11px; padding:15px 13px; border:0; border-bottom:1px solid #edf3f1; background:#fff; text-align:left; cursor:pointer; }
.article-list button:last-child { border-bottom:0; }
.order { width:30px; height:30px; flex:0 0 auto; display:grid; place-items:center; border-radius:10px; background:#e9f7f4; color:#0f766e; font-size:11px; font-weight:800; }
.article-copy { flex:1; min-width:0; }
.article-copy b { display:block; overflow:hidden; text-overflow:ellipsis; white-space:nowrap; color:#2e4642; font-size:13.5px; }
.article-copy small { display:block; margin-top:4px; color:#93a19e; font-size:10.5px; }
.arrow { color:#9cb0ac; font-size:20px; }
</style>
