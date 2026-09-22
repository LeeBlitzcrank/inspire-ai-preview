<template>
  <div class="following-page">
    <div class="topbar">
      <button class="back" @click="$router.back()">‹</button>
      <div class="ttl">我的关注</div>
      <div class="cnt">{{ list.length }} 人</div>
    </div>

    <AppState
      :state="listState"
      :rows="4"
      empty-icon="💭"
      empty-text="还没有关注的人"
      error-text="关注列表加载失败"
      @retry="load"
    >
      <AppCard
        v-for="u in list"
        :key="u.id"
        class="urow"
        padding="12px"
        clickable
        @click="goUser(u)"
      >
        <div class="uav" @click="goUser(u)">{{ avatarOf(u) }}</div>
        <div class="txt" @click="goUser(u)">
          <div class="un">{{ u.nickname || u.username }}</div>
          <div class="ua">@{{ u.username }}</div>
        </div>
        <div class="uops">
          <button @click.stop="toMessage(u)">私信</button>
          <button class="unfollow" @click.stop="doUnfollow(u)">取消关注</button>
        </div>
      </AppCard>
    </AppState>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import { ElMessage } from '@/utils/uiFeedback.js'
import { getFollowing, unfollowUser } from '@/api/inspire.js'
import { startConversation } from '@/api/message.js'

const router = useRouter()
const list = ref([])
const loading = ref(true)
const loadError = ref('')
const listState = computed(() => {
  if (loading.value) return 'loading'
  if (loadError.value) return 'error'
  return list.value.length ? 'ready' : 'empty'
})

const isImg = (a) => typeof a === 'string' && (a.startsWith('http') || a.startsWith('/') || a.startsWith('data:'))
const avatarOf = (u) => {
  if (u.avatar && !isImg(u.avatar)) return Array.from(String(u.avatar))[0]
  return Array.from(String(u.nickname || u.username || '灵'))[0]
}

const load = async () => {
  loading.value = true
  loadError.value = ''
  try {
    const res = await getFollowing()
    list.value = res.data || []
  } catch (e) {
    console.error(e)
    loadError.value = e?.message || 'load failed'
  } finally {
    loading.value = false
  }
}

const goUser = (u) => router.push(`/auth/user/public/${u.id}`)

const toMessage = async (u) => {
  try {
    const res = await startConversation(u.id)
    if (res.data?.id) router.push(`/messages?convId=${res.data.id}&direct=1`)
    else router.push('/messages')
  } catch (e) {
    ElMessage.error('打开私信失败')
  }
}

const doUnfollow = async (u) => {
  try {
    await ElMessageBox.confirm(`确定不再关注「${u.nickname || u.username}」？`, '提示', { type: 'warning' })
  } catch (e) { return }
  try {
    const res = await unfollowUser(u.id)
    if (res.code === 200) {
      list.value = list.value.filter(x => String(x.id) !== String(u.id))
      ElMessage.success('已取消关注')
    } else {
      ElMessage.error(res.msg || '操作失败')
    }
  } catch (e) {
    ElMessage.error('操作失败')
  }
}

onMounted(load)
</script>

<style scoped>
.following-page { max-width: 620px; margin: 0 auto; padding: 14px 16px 60px; background: #fbfcfe; min-height: 100vh; }
.topbar { display: flex; align-items: center; gap: 10px; padding: 4px 0 16px; }
.back { width: 36px; height: 36px; border-radius: 50%; border: none; background: #fff;
  color: #0f766e; font-size: 18px; cursor: pointer; box-shadow: 0 1px 6px rgba(0,0,0,.05); }
.ttl { font-size: 16px; font-weight: 700; }
.cnt { margin-left: auto; font-size: 12px; color: #9aa3b2; }
.urow { display: flex; align-items: center; gap: 11px; margin-bottom: 9px; border-radius: 14px; }
.uav { width: 44px; height: 44px; border-radius: 50%; background: #e6f2e4; color: #4f8a48;
  display: flex; align-items: center; justify-content: center; font-size: 19px; font-weight: 700;
  flex: 0 0 auto; cursor: pointer; }
.txt { min-width: 0; cursor: pointer; }
.un { font-size: 13.5px; font-weight: 600; margin-bottom: 3px; }
.ua { font-size: 11.5px; color: #9aa3b2; }
.uops { margin-left: auto; display: flex; gap: 7px; }
.uops button { font-size: 11.5px; padding: 6px 11px; border-radius: 999px;
  border: 1px solid #e3ecea; background: #fff; color: #0f766e; cursor: pointer; font-family: inherit; }
.uops button.unfollow { color: #9aa3b2; }
</style>
