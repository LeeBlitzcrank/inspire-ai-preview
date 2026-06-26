
// 离线模拟接口 - 适配Storm/ES/MQ业务前台预览
export const submitInspire = (data) => {
  return new Promise(resolve => {
    setTimeout(() => resolve({code:200,msg:"AI灵感任务提交成功！"}), 800)
  })
}

// 模拟Storm实时热度榜单
export const getHeatInspireList = () => {
  return new Promise(resolve => {
    const list = [
      {id:1,title:"家常鸡腿做法大全",content:"包含清炖、红烧、油炸、卤制多种做法，适配家庭日常烹饪，简单易学、食材常见、省时省力。",viewCount:1234,collectCount:233,heatScore:987},
      {id:2,title:"短视频文案万能模板",content:"适用于生活、美食、情感、好物分享类短视频，句式高级、节奏紧凑、完播率高。",viewCount:2241,collectCount:532,heatScore:1250},
      {id:3,title:"夏日穿搭灵感",content:"简约清爽风格搭配方案，配色干净、显瘦百搭，适合学生党、日常通勤穿搭参考。",viewCount:988,collectCount:166,heatScore:760}
    ]
    setTimeout(()=>resolve({code:200,data:list}),300)
  })
}

// 模拟ES全文检索
export const searchInspire = ({keyword}) => {
  return new Promise(resolve => {
    const list = [{id:4,title:`${keyword} 创意拓展方案`,content:"根据关键词智能生成多维灵感思路，覆盖玩法、技巧、避坑、进阶方案。",viewCount:345,collectCount:88,heatScore:420}]
    setTimeout(()=>resolve({code:200,data:list}),200)
  })
}

// 收藏行为（模拟Storm行为上报触发热度更新）
export const collectInspire = () => {
  return new Promise(resolve => resolve({code:200,msg:"收藏成功，已同步至公共灵感池"}))
}

// 个人私有灵感
export const getPersonalInspire = () => {
  return new Promise(resolve => {
    const list = [{id:10,title:"我的美食灵感记录",content:"日常家常菜创新做法、简易烹饪技巧汇总。",viewCount:22,collectCount:5,heatScore:66}]
    setTimeout(()=>resolve({code:200,data:list}),200)
  })
}

// 个人Storm实时统计数据
export const getPersonalStat = () => {
  return new Promise(resolve => {
    const stat = {inspireCount:12,collectCount:35,viewTotal:1289}
    setTimeout(()=>resolve({code:200,data:stat}),200)
  })
}
