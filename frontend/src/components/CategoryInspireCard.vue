<template>
  <AppCard class="category-inspire-card" padding="0" clickable @click="goDetail">
    <div class="category-cover">
      <img v-if="item.img" :src="thumbOf(item.img, 400)" :alt="item.title" loading="lazy" decoding="async">
      <div v-else class="cover-placeholder">{{ item.tag || '灵感' }}</div>
      <span v-if="item.tag" class="cover-tag">{{ item.tag }}</span>
    </div>
    <div class="category-copy">
      <h3>{{ item.title }}</h3>
      <p v-if="item.content">{{ item.content }}</p>
      <div class="category-meta">
        <span class="heat">{{ item.heat || 0 }} 热度</span>
        <span>{{ item.viewCount || 0 }} 浏览</span>
        <button type="button" class="collect-button" @click.stop="$emit('collect', item.id)">
          ♡ 收藏
        </button>
      </div>
    </div>
  </AppCard>
</template>

<script setup>
import AppCard from '@/components/base/AppCard.vue'
import {thumbOf} from '@/utils/media.js'
import {useRouter} from 'vue-router'

const props = defineProps({
  item: {type: Object, required: true}
})
defineEmits(['collect'])
const router = useRouter()

const goDetail = () => {
  if (props.item.id !== null && props.item.id !== undefined && String(props.item.id).trim()) {
    router.push({name: 'InspireDetail', params: {id: String(props.item.id)}})
  }
}
</script>

<style scoped>
.category-inspire-card { overflow:hidden; border-radius:17px; }
.category-cover { position:relative; height:148px; overflow:hidden; background:#e5ece9; }
.category-cover img { display:block; width:100%; height:100%; object-fit:cover; }
.category-cover:after { content:""; position:absolute; inset:0; background:linear-gradient(180deg,transparent 48%,rgba(10,28,23,.48)); pointer-events:none; }
.cover-placeholder { height:100%; display:grid; place-items:center; color:#668079; font-size:20px; font-weight:800; }
.cover-tag { position:absolute; z-index:2; left:11px; bottom:10px; padding:4px 8px; border-radius:999px; background:rgba(255,255,255,.86); color:#35685d; font-size:9px; font-weight:850; backdrop-filter:blur(8px); }
.category-copy { padding:11px 12px 12px; }
.category-copy h3 { margin:0; color:#1d1d1f; font-size:14px; font-weight:650; line-height:1.45; }
.category-copy p { display:-webkit-box; margin:6px 0 0; overflow:hidden; color:#7e8d88; font-size:10.5px; line-height:1.6; -webkit-box-orient:vertical; -webkit-line-clamp:2; }
.category-meta { display:flex; align-items:center; gap:10px; margin-top:10px; color:#98a39f; font-size:9px; }
.category-meta .heat { color:#a76532; font-weight:850; }
.collect-button { margin-left:auto; padding:3px 0 3px 8px; border:0; background:transparent; color:#1f6d5c; font:inherit; font-size:9.5px; font-weight:850; cursor:pointer; }
</style>
