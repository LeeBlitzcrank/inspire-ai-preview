/**
 * 文件：frontend/src/pages/personal/composables/usePersonalProfile.js
 * 所属模块：用户端页面和交互流程
 * 主要职责：Vue Composable，集中管理页面状态、异步流程和生命周期
 * 维护说明：注释解释文件边界和核心意图，具体业务规则以方法、组件和主文档说明为准。
 * INSPIRE_FILE_HEADER
 */
import {nextTick, onBeforeUnmount, ref, watch} from 'vue'
import {ElMessage} from '@/utils/uiFeedback.js'
import {changePassword, updateUserInfo} from '@/api/inspire.js'
import {bindPhone, getIpLocation, sendSmsCode} from '@/api/auth.js'
import {findCityPath} from '@/utils/cityData.js'
import {validateNickname, validatePassword} from '@/utils/validation.js'
import {clearAllTokens} from '@/utils/tokenStorage.js'

export function usePersonalProfile(userInfo) {
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
      // 96 个头像要滚动，打开时主动定位到当前选中的那个（没有选中就回到顶部）
      nextTick(() => {
        const grid = document.querySelector('.avatar-grid')
        if (!grid) return
        const selected = grid.querySelector('.avatar-option.selected')
        if (selected) selected.scrollIntoView({ block: 'nearest' })
        else grid.scrollTop = 0
      })
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
    const body = await getIpLocation()
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
    } catch (e) {
      console.error(e)
    }
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

  const bindForm = ref({ phone: '', code: '' })
  const bindingPhone = ref(false)
  const smsCodeSending = ref(false)
  const smsCountdown = ref(0)
  let smsTimer = null

  const startSmsCountdown = (seconds = 60) => {
    smsCountdown.value = Math.max(1, Number(seconds) || 60)
    clearInterval(smsTimer)
    smsTimer = setInterval(() => {
      smsCountdown.value -= 1
      if (smsCountdown.value <= 0) clearInterval(smsTimer)
    }, 1000)
  }

  onBeforeUnmount(() => clearInterval(smsTimer))

  const handleSendBindCode = async () => {
    if (!/^1[3-9]\d{9}$/.test(bindForm.value.phone.trim())) {
      return ElMessage.warning('请输入正确的11位手机号')
    }
    smsCodeSending.value = true
    try {
      const res = await sendSmsCode(bindForm.value.phone.trim(), 'bind')
      if (res.code !== 200) return ElMessage.error(res.msg || '验证码发送失败')
      if (res.data?.devCode) {
        bindForm.value.code = res.data.devCode
        ElMessage.success(`开发验证码 ${res.data.devCode} 已自动填入`)
      } else {
        ElMessage.success('验证码已发送')
      }
      startSmsCountdown(res.data?.cooldownSeconds)
    } catch (e) {
      // 请求层已提示
    } finally {
      smsCodeSending.value = false
    }
  }

  const handleBindPhone = async () => {
    if (!/^1[3-9]\d{9}$/.test(bindForm.value.phone.trim())) {
      return ElMessage.warning('请输入正确的11位手机号')
    }
    if (!/^\d{6}$/.test(bindForm.value.code.trim())) {
      return ElMessage.warning('请输入6位短信验证码')
    }
    bindingPhone.value = true
    try {
      const res = await bindPhone(bindForm.value.phone.trim(), bindForm.value.code.trim())
      if (res.code === 200) {
        userInfo.value.phone = bindForm.value.phone.trim()
        bindForm.value = { phone: '', code: '' }
        ElMessage.success('手机号绑定成功')
      } else {
        ElMessage.error(res.msg || '绑定失败')
      }
    } catch (e) {
      // 请求层已提示
    } finally {
      bindingPhone.value = false
    }
  }

  const handleSaveProfile = async () => {
    const nicknameError = validateNickname(editForm.value.nickname)
    if (nicknameError) return ElMessage.warning(nicknameError)
    savingProfile.value = true
    try {
      const res = await updateUserInfo(editForm.value)
      if (res.code === 200) {
        userInfo.value.nickname = editForm.value.nickname
        userInfo.value.city = editForm.value.city
        userInfo.value.avatar = editForm.value.avatar
        if (editForm.value.avatar) sessionStorage.setItem('userAvatar', editForm.value.avatar)
        if (editForm.value.nickname) sessionStorage.setItem('userAccount', editForm.value.nickname)
        ElMessage.success('资料已更新')
        showProfileDialog.value = false
      } else {
        ElMessage.error(res.msg || '资料更新失败')
      }
    } catch (e) {
      ElMessage.error(e?.response?.data?.msg || e?.message || '资料更新失败')
    } finally {
      savingProfile.value = false
    }
  }

  const handleChangePassword = async () => {
    if (!pwdForm.value.oldPassword) return ElMessage.warning('请输入旧密码')
    if (!pwdForm.value.newPassword) return ElMessage.warning('请输入新密码')
    if (pwdForm.value.newPassword !== pwdForm.value.confirmPassword) return ElMessage.warning('两次密码不一致')
    const passwordError = validatePassword(pwdForm.value.newPassword, {
      username: userInfo.value.username || '',
      email: userInfo.value.email || ''
    })
    if (passwordError) return ElMessage.warning(passwordError)
    savingPwd.value = true
    try {
      const res = await changePassword(pwdForm.value)
      if (res.code === 200) {
        ElMessage.success('密码修改成功，请重新登录')
        pwdForm.value = { oldPassword: '', newPassword: '', confirmPassword: '' }
        showPwdDialog.value = false
        clearAllTokens()
        window.setTimeout(() => {
          window.location.href = '/#/login'
        }, 600)
      } else {
        ElMessage.error(res.msg || '密码修改失败')
      }
    } catch (e) {
      ElMessage.error(e?.response?.data?.msg || e?.message || '密码修改失败')
    } finally {
      savingPwd.value = false
    }
  }

  return {
    showProfileDialog,
    savingProfile,
    editForm,
    cityPath,
    autoDetecting,
    detectCity,
    onCityChange,
    detectLocation,
    applyDetectedCity,
    showPwdDialog,
    savingPwd,
    pwdForm,
    bindForm,
    bindingPhone,
    smsCodeSending,
    smsCountdown,
    handleSendBindCode,
    handleBindPhone,
    handleSaveProfile,
    handleChangePassword
  }
}
