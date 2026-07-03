<template>
  <div class="personal-page">
    <div class="top-nav">
      <div class="left-logo" @click="$router.push('/')">🍎</div>
      <div class="right-icons">
        <div class="icon-item" @click="$router.push('/create')">✨</div>
        <div class="icon-item" @click="$router.push('/search')">🔍</div>
      </div>
    </div>

    <!-- 用户信息 -->
    <div class="user-header" @click="showProfileDialog = true">
      <div class="avatar">{{ userInfo.avatar || (userInfo.nickname ? userInfo.nickname[0] : '👤') }}</div>
      <div class="user-info">
        <h2>{{ userInfo.nickname || userInfo.username || '灵感爱好者' }}</h2>
        <p v-if="userInfo.email">{{ userInfo.email }}</p>
        <p v-if="userInfo.city" class="city-tag">{{ userInfo.city }}</p>
      </div>
      <div class="edit-icon">✏️</div>
    </div>

    <!-- 统计 -->
    <div class="stat-wrap">
      <div class="stat-card" v-for="item in statList" :key="item.id">
        <div class="stat-num">{{ item.num }}</div>
        <div class="stat-line"></div>
        <div class="stat-label">{{ item.label }}</div>
      </div>
    </div>

    <!-- 快捷操作 -->
    <div class="quick-actions">
      <div class="action-btn" @click="showProfileDialog = true">👤 编辑资料</div>
      <div class="action-btn" @click="showPwdDialog = true">🔒 修改密码</div>
    </div>

    <!-- 选项卡 -->
    <div class="tab-bar">
      <div class="tab-item" :class="{active: activeTab==='published'}" @click="switchTab('published')">我的发布</div>
      <div class="tab-item" :class="{active: activeTab==='drafts'}" @click="switchTab('drafts')">我的草稿</div>
      <div class="tab-item" :class="{active: activeTab==='collects'}" @click="switchTab('collects')">我的收藏</div>
    </div>

    <div v-if="loading" class="empty-sub"><div v-for="n in 3" :key="n" class="skeleton-card" style="padding:20px"><div class="s-line s-w-60"></div><div class="s-line s-w-90"></div><div class="s-line s-w-40"></div></div></div>

    <!-- 发布列表 -->
    <div class="list-wrap" v-if="activeTab==='published'">
      <InspireCard v-for="item in publishedList" :key="item.id" :item="item" />
      <div v-if="publishedList.length === 0 && !loading" class="empty-sub">✍️ 还没有发布过灵感</div>
    </div>

    <!-- 草稿列表 -->
    <div class="list-wrap" v-if="activeTab==='drafts'">
      <div v-for="item in draftList" :key="item.id" class="draft-card" @click="$router.push('/edit/' + item.id)">
        <div class="draft-title">{{ item.title || '无标题' }}</div>
        <div class="draft-meta">{{ item.tag }} &middot; {{ formatTime(item.createTime) }}</div>
      </div>
      <div v-if="draftList.length === 0 && !loading" class="empty-sub">📝 还没有草稿</div>
    </div>

    <!-- 收藏列表 -->
    <div class="list-wrap" v-if="activeTab==='collects'">
      <InspireCard v-for="item in collectList" :key="item.id" :item="item" @collect="handleUncollect" />
      <div v-if="collectList.length === 0 && !loading" class="empty-sub">⭐ 还没有收藏过灵感</div>
    </div>

    <div class="logout-wrap">
      <el-button type="link" class="logout-btn" @click="handleLogout">退出登录</el-button>
    </div>

    <!-- 编辑资料对话框 -->
    <el-dialog v-model="showProfileDialog" title="编辑资料" width="90%" max-width="420px">
      <div class="dialog-form">
        <div class="form-row">
          <label>头像</label>
          <div class="avatar-grid">
            <div v-for="a in avatarList" :key="a" class="avatar-option" :class="{selected: editForm.avatar === a}" @click="editForm.avatar = a">
              {{ a }}
            </div>
          </div>
        </div>
        <div class="form-row">
          <label>昵称</label>
          <div style="display:flex;gap:8px"><el-input v-model="editForm.nickname" placeholder="输入昵称" maxlength="20" /><el-button size="small" @click="editForm.nickname = randomNickname()">🎲</el-button></div>
        </div>
        <div class="form-row">
          <label>城市</label>
          <el-cascader v-model="cityPath" :options="cityOptions" placeholder="搜索或选择城市" clearable filterable :props="{ expandTrigger: 'hover' }" style="width:100%;max-width:280px;" popper-class="city-popper" @change="onCityChange" />
          <div v-if="autoDetecting" class="detect-hint">⏳ 正在自动定位...</div>
          <div v-if="detectCity && !cityPath.length" class="detect-hint">📍 检测到您可能在 <el-link type="primary" @click="applyDetectedCity">{{ detectCity }}</el-link>，点击使用</div>
        </div>
      </div>
      <template #footer>
        <el-button @click="showProfileDialog = false">取消</el-button>
        <el-button type="primary" :loading="savingProfile" @click="handleSaveProfile">保存</el-button>
      </template>
    </el-dialog>

    <!-- 修改密码对话框 -->
    <el-dialog v-model="showPwdDialog" title="修改密码" width="90%" max-width="400px">
      <div class="dialog-form">
        <div class="form-row">
          <label>旧密码</label>
          <el-input v-model="pwdForm.oldPassword" type="password" placeholder="输入旧密码" show-password />
        </div>
        <div class="form-row">
          <label>新密码</label>
          <el-input v-model="pwdForm.newPassword" type="password" placeholder="输入新密码" show-password />
        </div>
        <div class="form-row">
          <label>确认新密码</label>
          <el-input v-model="pwdForm.confirmPassword" type="password" placeholder="再次输入新密码" show-password />
        </div>
      </div>
      <template #footer>
        <el-button @click="showPwdDialog = false">取消</el-button>
        <el-button type="primary" :loading="savingPwd" @click="handleChangePassword">确认修改</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, watch, onMounted } from 'vue'
import InspireCard from '@/components/InspireCard.vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getUserInfo, getMyInspires, getMyCollects, getMyDrafts,
         uncollectInspire, updateUserInfo, changePassword } from '@/api/inspire'
import { cityOptions, findCityPath } from '@/utils/cityData'
import { randomNickname } from '@/utils/nickname'

const router = useRouter()
const userInfo = ref({})
const publishedList = ref([])
const draftList = ref([])
const collectList = ref([])
const loading = ref(false)
const activeTab = ref('published')

const statList = ref([
  { id: 1, num: 0, label: '总发布' },
  { id: 2, num: 0, label: '总收藏' },
  { id: 3, num: 0, label: '总浏览量' }
])

const avatarList = ['😊', '😎', '🤩', '😍', '😜', '🤠', '🌈', '🐻', '🐶', '🐱', '🐸', '🐮', '🌴', '🌺', '🔥', '🌟', '💡', '💻', '🎨', '🎬']
// 编辑资料
const showProfileDialog = ref(false)
const savingProfile = ref(false)
const editForm = ref({ nickname: '', city: '', avatar: '' })

// When dialog opens, populate form from userInfo
watch(showProfileDialog, (val) => {
  if (val) {
    editForm.value.nickname = userInfo.value.nickname || ''
    editForm.value.city = userInfo.value.city || ''
    editForm.value.avatar = userInfo.value.avatar || ''
    const existingPath = findCityPath(userInfo.value.city)
    cityPath.value = existingPath
    detectLocation()
  }
})
const cityPath = ref([])
const autoDetecting = ref(false)
const detectCity = ref('')

const onCityChange = (val) => {
  // val = ['浙江', '杭州'] 或 直辖市: ['北京', '北京']
  if (val && val.length >= 2) editForm.value.city = val[1]
  else if (val && val.length === 1) editForm.value.city = val[0]
  else editForm.value.city = ''
}

// 英文 -> 中文城市名映射
const enToCn = {
  'beijing': '北京', 'shanghai': '上海', 'tianjin': '天津', 'chongqing': '重庆',
  'hongkong': '香港', 'macau': '澳门', 'macao': '澳门', 'taipei': '台北', 'kaohsiung': '高雄', 'taichung': '台中', 'tainan': '台南', 'newtaipei': '新北', 'taoyuan': '桃园', 'keelung': '基隆',
  'guangzhou': '广州', 'shenzhen': '深圳', 'zhuhai': '珠海', 'shantou': '汕头', 'foshan': '佛山', 'shaoguan': '韶关', 'zhanjiang': '湛江', 'zhaoqing': '肇庆', 'jiangmen': '江门',
  'maoming': '茂名', 'huizhou': '惠州', 'meizhou': '梅州', 'shanwei': '汕尾', 'heyuan': '河源', 'yangjiang': '阳江', 'qingyuan': '清远', 'dongguan': '东莞', 'zhongshan': '中山',
  'chaozhou': '潮州', 'jieyang': '揭阳', 'yunfu': '云浮', 'hangzhou': '杭州', 'ningbo': '宁波', 'wenzhou': '温州', 'jiaxing': '嘉兴', 'huzhou': '湖州', 'shaoxing': '绍兴',
  'jinhua': '金华', 'quzhou': '衢州', 'zhoushan': '舟山', 'taizhou': '台州', 'lishui': '丽水', 'nanjing': '南京', 'wuxi': '无锡', 'xuzhou': '徐州', 'changzhou': '常州',
  'suzhou': '苏州', 'nantong': '南通', 'lianyungang': '连云港', 'huai_an': '淮安', 'yancheng': '盐城', 'yangzhou': '扬州', 'zhenjiang': '镇江', 'taizhou_j': '泰州', 'suqian': '宿迁',
  'jinan': '济南', 'qingdao': '青岛', 'zibo': '淄博', 'zaozhuang': '枣庄', 'dongying': '东营', 'yantai': '烟台', 'weifang': '潍坊', 'jining': '济宁', 'tai_an': '泰安',
  'weihai': '威海', 'rizhao': '日照', 'linyi': '临沂', 'dezhou': '德州', 'liaocheng': '聊城', 'binzhou': '滨州', 'heze': '菏泽', 'chengdu': '成都', 'zigong': '自贡', 'panzhihua': '攀枝花',
  'luzhou': '泸州', 'deyang': '德阳', 'mianyang': '绵阳', 'guangyuan': '广元', 'suining': '遂宁', 'neijiang': '内江', 'leshan': '乐山', 'nanchong': '南充', 'meishan': '眉山',
  'yibin': '宜宾', 'guang_an': '广安', 'dazhou': '达州', 'ya_an': '雅安', 'bazhong': '巴中', 'ziyang': '资阳', 'wuhan': '武汉', 'huangshi': '黄石', 'shiyan': '十堰', 'yichang': '宜昌',
  'xiangyang': '襄阳', 'ezhou': '鄂州', 'jingmen': '荆门', 'xiaogan': '孝感', 'jingzhou': '荆州', 'huanggang': '黄冈', 'xianning': '咸宁', 'suizhou': '随州', 'changsha': '长沙',
  'zhuzhou': '株洲', 'xiangtan': '湘潭', 'hengyang': '衡阳', 'shaoyang': '邵阳', 'yueyang': '岳阳', 'changde': '常德', 'zhangjiajie': '张家界', 'yiyang': '益阳', 'chenzhou': '郴州',
  'yongzhou': '永州', 'huaihua': '怀化', 'loudi': '娄底', 'fuzhou': '福州', 'xiamen': '厦门', 'putian': '莆田', 'sanming': '三明', 'quanzhou': '泉州', 'zhangzhou': '漳州',
  'nanping': '南平', 'longyan': '龙岩', 'ningde': '宁德', 'zhengzhou': '郑州', 'kaifeng': '开封', 'luoyang': '洛阳', 'pingdingshan': '平顶山', 'anyang': '安阳', 'hebi': '鹤壁',
  'xinxiang': '新乡', 'jiaozuo': '焦作', 'puyang': '濮阳', 'xuchang': '许昌', 'luohe': '漯河', 'sanmenxia': '三门峡', 'nanyang': '南阳', 'shangqiu': '商丘', 'xinyang': '信阳',
  'zhoukou': '周口', 'zhumadian': '驻马店', 'hefei': '合肥', 'wuhu': '芜湖', 'bengbu': '蚌埠', 'huainan': '淮南', 'ma_anshan': '马鞍山', 'huaibei': '淮北', 'tongling': '铜陵',
  'anqing': '安庆', 'huangshan': '黄山', 'chuzhou': '滁州', 'fuyang': '阜阳', 'suzhou_a': '宿州', 'lu_an': '六安', 'bozhou': '亳州', 'chizhou': '池州', 'xuancheng': '宣城',
  'shijiazhuang': '石家庄', 'tangshan': '唐山', 'qinhuangdao': '秦皇岛', 'handan': '邯郸', 'xingtai': '邢台', 'baoding': '保定', 'zhangjiakou': '张家口', 'chengde': '承德',
  'cangzhou': '沧州', 'langfang': '廊坊', 'hengshui': '衡水', 'xi_an': '西安', 'tongchuan': '铜川', 'baoji': '宝鸡', 'xianyang': '咸阳', 'weinan': '渭南', 'yan_an': '延安',
  'hanzhong': '汉中', 'yulin': '榆林', 'ankang': '安康', 'shangluo': '商洛', 'taiyuan': '太原', 'datong': '大同', 'yangquan': '阳泉', 'changzhi': '长治', 'jincheng': '晋城',
  'shuozhou': '朔州', 'jinzhong': '晋中', 'yuncheng': '运城', 'xinzhou': '忻州', 'linfen': '临汾', 'lvliang': '吕梁', 'shenyang': '沈阳', 'dalian': '大连', 'anshan': '鞍山',
  'fushun': '抚顺', 'benxi': '本溪', 'dandong': '丹东', 'jinzhou': '锦州', 'yingkou': '营口', 'fuxin': '阜新', 'liaoyang': '辽阳', 'panjin': '盘锦', 'tieling': '铁岭',
  'chaoyang': '朝阳', 'huludao': '葫芦岛', 'changchun': '长春', 'jilin': '吉林', 'siping': '四平', 'liaoyuan': '辽源', 'tonghua': '通化', 'baishan': '白山', 'songyuan': '松原',
  'baicheng': '白城', 'harbin': '哈尔滨', 'qiqihar': '齐齐哈尔', 'jixi': '鸡西', 'hegang': '鹤岗', 'shuangyashan': '双鸭山', 'daqing': '大庆', 'yichun': '伊春', 'jiamusi': '佳木斯',
  'qitaihe': '七台河', 'mudanjiang': '牡丹江', 'heihe': '黑河', 'suihua': '绥化', 'nanchang': '南昌', 'jingdezhen': '景德镇', 'pingxiang': '萍乡', 'jiujiang': '九江', 'xinyu': '新余',
  'yingtan': '鹰潭', 'ganzhou': '赣州', 'ji_an': '吉安', 'yichun_j': '宜春', 'fuzhou_j': '抚州', 'shangrao': '上饶', 'nanning': '南宁', 'liuzhou': '柳州', 'guilin': '桂林',
  'wuzhou': '梧州', 'beihai': '北海', 'fangchenggang': '防城港', 'qinzhou': '钦州', 'guigang': '贵港', 'yulin_g': '玉林', 'baise': '百色', 'hezhou': '贺州', 'hechi': '河池',
  'laibin': '来宾', 'chongzuo': '崇左', 'kunming': '昆明', 'qujing': '曲靖', 'yuxi': '玉溪', 'baoshan': '保山', 'zhaotong': '昭通', 'lijiang': '丽江', 'pu_er': '普洱', 'lincang': '临沧',
  'chuxiong': '楚雄', 'honghe': '红河', 'wenshan': '文山', 'xishuangbanna': '西双版纳', 'dali': '大理', 'dehong': '德宏', 'nujiang': '怒江', 'diqing': '迪庆',
  'guiyang': '贵阳', 'liupanshui': '六盘水', 'zunyi': '遵义', 'anshun': '安顺', 'bijie': '毕节', 'tongren': '铜仁', 'qianxinan': '黔西南', 'qiandongnan': '黔东南', 'qiannan': '黔南',
  'lanzhou': '兰州', 'jiayuguan': '嘉峪关', 'jinchang': '金昌', 'baiyin': '白银', 'tianshui': '天水', 'wuwei': '武威', 'zhangye': '张掖', 'pingliang': '平凉', 'jiuquan': '酒泉',
  'qingyang': '庆阳', 'dingxi': '定西', 'longnan': '陇南', 'hohhot': '呼和浩特', 'baotou': '包头', 'wuhai': '乌海', 'chifeng': '赤峰', 'tongliao': '通辽', 'ordos': '鄂尔多斯',
  'hulunbuir': '呼伦贝尔', 'binyan': '巴彦淖尔', 'wulanqab': '乌兰察布', 'urumqi': '乌鲁木齐', 'karamay': '克拉玛依', 'turpan': '吐鲁番', 'hami': '哈密',
  'haikou': '海口', 'sanya': '三亚', 'sansha': '三沙', 'danzhou': '儋州',
  'lhasa': '拉萨', 'shigatse': '日喀则', 'xining': '西宁', 'yinchuan': '银川', 'shizuishan': '石嘴山', 'wuzhong': '吴忠', 'guyuan': '固原', 'zhongwei': '中卫'
}
const toCn = (s) => enToCn[(s || '').toLowerCase().replace(/[\s'_-]/g, '')] || s

const detectLocation = async () => {
  autoDetecting.value = true
  try {
    const res = await fetch('/api/auth/ip-location', {
      headers: { 'Authorization': 'Bearer ' + (localStorage.getItem('token') || '') }
    })
    if (res.ok) {
      const body = await res.json()
      if (body && body.code === 200 && body.data) {
        const d = body.data
        const city = toCn(d.city || d.region)
        const path = findCityPath(city)
        if (path.length > 0) {
          cityPath.value = path
          onCityChange(path)
          detectCity.value = path.join(' / ')
        }
      }
    }
  } catch (e) {}
  finally { autoDetecting.value = false }
}

const applyDetectedCity = () => {
  if (cityPath.value.length > 0) {
    onCityChange(cityPath.value)
    detectCity.value = ''
  }
}

// 修改密码
const showPwdDialog = ref(false)
const savingPwd = ref(false)
const pwdForm = ref({ oldPassword: '', newPassword: '', confirmPassword: '' })

onMounted(async () => {
  loading.value = true
  try {
    const [userRes, pubRes, colRes] = await Promise.all([
      getUserInfo(), getMyInspires(), getMyCollects()
    ])
    userInfo.value = userRes.data || {}
    if (userRes.data?.avatar) localStorage.setItem('userAvatar', userRes.data.avatar)
    publishedList.value = pubRes.data || []
    collectList.value = colRes.data || []
    statList.value[0].num = publishedList.value.length
    statList.value[1].num = collectList.value.length
    statList.value[2].num = publishedList.value.reduce((s, i) => s + (i.viewCount || 0), 0)
  } catch (e) {}
  finally { loading.value = false }
})

const switchTab = async (tab) => {
  activeTab.value = tab
  if (tab === 'collects' && collectList.value.length === 0) {
    try {
      const res = await getMyCollects()
      collectList.value = res.data || []
      statList.value[1].num = collectList.value.length
    } catch (e) {}
  }
  if (tab === 'drafts') {
    try {
      const res = await getMyDrafts()
      draftList.value = res.data || []
      console.log('[DRAFTS] 加载完成, 数量:', draftList.value.length)
    } catch (e) {}
  }
}

const handleUncollect = async (id) => {
  try {
    await uncollectInspire(id)
    collectList.value = collectList.value.filter(i => i.id !== id)
    statList.value[1].num = collectList.value.length
    ElMessage.success('已取消收藏')
  } catch (e) {}
}

const handleSaveProfile = async () => {
  savingProfile.value = true
  try {
    const res = await updateUserInfo(editForm.value)
    if (res.code === 200) {
      userInfo.value.nickname = editForm.value.nickname
      userInfo.value.city = editForm.value.city
      userInfo.value.avatar = editForm.value.avatar
      if (editForm.value.avatar) localStorage.setItem('userAvatar', editForm.value.avatar)
      if (editForm.value.nickname) localStorage.setItem('userAccount', editForm.value.nickname)
      ElMessage.success('资料已更新')
      showProfileDialog.value = false
    }
  } catch (e) {}
  finally { savingProfile.value = false }
}

const handleChangePassword = async () => {
  if (!pwdForm.value.oldPassword) return ElMessage.warning('请输入旧密码')
  if (!pwdForm.value.newPassword) return ElMessage.warning('请输入新密码')
  if (pwdForm.value.newPassword !== pwdForm.value.confirmPassword) return ElMessage.warning('两次密码不一致')
  savingPwd.value = true
  try {
    const res = await changePassword(pwdForm.value)
    if (res.code === 200) {
      ElMessage.success('密码修改成功')
      pwdForm.value = { oldPassword: '', newPassword: '', confirmPassword: '' }
      showPwdDialog.value = false
    }
  } catch (e) {}
  finally { savingPwd.value = false }
}

const formatTime = (t) => {
  if (!t) return ''
  const d = new Date(t)
  return d.toLocaleDateString('zh-CN')
}

const handleLogout = () => {
  localStorage.removeItem('token'); localStorage.removeItem('isLogin')
  localStorage.removeItem('userAccount'); localStorage.removeItem('userId')
  localStorage.removeItem('adminToken'); localStorage.removeItem('adminUser')
  ElMessage.success('已退出登录'); router.push('/login')
}
</script>

<style scoped>
.personal-page { width:94%; max-width:620px; margin:0 auto; padding:16px 0 80px; background:#fbfcfe; min-height:100vh; }
.top-nav { display:flex; justify-content:space-between; align-items:center; padding:8px 16px 16px; }
.left-logo { font-size:26px; cursor:pointer; width:40px; height:40px; display:flex; align-items:center; justify-content:center; border-radius:50%; background:#fff; box-shadow:0 1px 6px rgba(0,0,0,0.05); }
.right-icons { display:flex; gap:20px; }
.icon-item { width:40px; height:40px; border-radius:50%; background:#fff; display:flex; align-items:center; justify-content:center; font-size:20px; cursor:pointer; box-shadow:0 1px 6px rgba(0,0,0,0.05); }
.user-header { display:flex; align-items:center; gap:16px; margin-bottom:24px; cursor:pointer; position:relative; }
.avatar { width:64px; height:64px; border-radius:50%; background:linear-gradient(135deg,#d4c5a9,#b8a88c); color:#fff; display:flex; align-items:center; justify-content:center; font-size:24px; font-weight:500; }
.user-info h2 { margin:0; font-size:20px; color:#1d1d1f; }
.user-info p { margin:4px 0 0; font-size:14px; color:#86868b; }
.city-tag { display:inline-block; background:#f0f9eb; color:#67c23a; font-size:12px; padding:2px 8px; border-radius:10px; margin-top:4px; }
:deep(.city-popper) { --el-cascader-menu-min-width: 100px; }
.edit-icon { margin-left:auto; padding-right:12px; font-size:18px; color:#c0c4cc; }
.stat-wrap { display:grid; grid-template-columns:repeat(3,1fr); gap:12px; margin-bottom:16px; }
.stat-card { background:#fff; border-radius:14px; padding:20px 10px; text-align:center; }
.stat-num { font-size:24px; font-weight:500; color:#1d1d1f; }
.stat-line { width:30px; height:2px; background:#409eff; margin:10px auto; }
.stat-label { font-size:13px; color:#86868b; }
.quick-actions { display:flex; gap:12px; margin-bottom:20px; }
.action-btn { flex:1; background:#fff; border-radius:12px; padding:12px; text-align:center; font-size:14px; color:#333; cursor:pointer; transition:0.2s; border:1px solid #f0f3f9; }
.action-btn:hover { border-color:#409eff; color:#409eff; }
.tab-bar { display:flex; background:#f4f7fd; border-radius:12px; padding:4px; margin-bottom:20px; }
.tab-item { flex:1; text-align:center; padding:10px; font-size:15px; color:#666; cursor:pointer; border-radius:10px; transition:0.25s; }
.tab-item.active { background:#fff; color:#409eff; font-weight:500; box-shadow:0 1px 4px rgba(0,0,0,0.06); }
.list-wrap { display:flex; flex-direction:column; gap:14px; }
.draft-card { background:#fff; border-radius:14px; padding:16px; cursor:pointer; transition:0.2s; border:1px solid #f0f3f9; }
.draft-card:hover { border-color:#409eff; }
.draft-title { font-size:15px; font-weight:500; color:#1d1d1f; margin-bottom:6px; }
.draft-meta { font-size:13px; color:#909399; }
.logout-wrap { margin-top:40px; text-align:center; }
.logout-btn { color:#f56c6c; font-size:14px; }
.empty-sub { text-align:center; color:#999; font-size:14px; padding:20px 0; }
.skeleton-card { border-radius:16px; border:1px solid #f0f3f9; background:#fff; }
.s-line { height:14px; border-radius:8px; background:linear-gradient(90deg,#f0f0f0 25%,#e8e8e8 50%,#f0f0f0 75%); background-size:200px 100%; animation:shimmer 1.5s infinite; margin-bottom:12px; }
.s-w-40 { width:40%; } .s-w-60 { width:60%; } .s-w-90 { width:90%; }
.dialog-form { padding:8px 0; }
.detect-hint { font-size:12px; color:#909399; margin-top:4px; }
.avatar-grid { display:grid; grid-template-columns:repeat(7,1fr); gap:6px; }
.avatar-option { width:36px; height:36px; border-radius:50%; display:flex; align-items:center; justify-content:center; font-size:20px; cursor:pointer; border:2px solid transparent; transition:0.2s; }
.avatar-option:hover { border-color:#409eff; }
.avatar-option.selected { border-color:#409eff; background:#ecf5ff; }
.form-row { margin-bottom:16px; }
.form-row label { display:block; font-size:13px; color:#666; margin-bottom:4px; }
@keyframes shimmer { 0%{background-position:-200px 0} 100%{background-position:calc(200px + 100%) 0} }
</style>
