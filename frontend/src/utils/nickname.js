// 随机昵称生成器（4字：形容词+名词）
const adj = ['快乐','聪明','勇敢','温柔','可爱','阳光','清风','明月','星辰','大海',
  '梦幻','甜蜜','俏皮','灵动','清新','文艺','酷炫','温暖','淡雅','宁静']
const noun = ['小鱼','小猫','小鹿','蝴蝶','樱花','奶茶','豆浆','包子','饼干','糖果',
  '花猫','松鼠','兔子','羊羔','熊猫','布丁','甜甜','冰淇淋','棉花糖','小幸福']

export function randomNickname() {
  const a = adj[Math.floor(Math.random() * adj.length)]
  const n = noun[Math.floor(Math.random() * noun.length)]
  return a + n
}
