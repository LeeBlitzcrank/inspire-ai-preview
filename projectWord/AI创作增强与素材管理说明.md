# AI 创作增强与素材管理说明

> 更新时间：2026-09-23
> 覆盖：AI 改写、AI 标题、草稿自动保存、系列合集、灵感模板预览、图片增强
> 说明：系列/合集已接入正式数据模型和演示数据；灵感模板当前仍为交互预览。

## 一、完成状态

| 功能 | 当前状态 |
| --- | --- |
| AI 改写/润色 | 正式实现 |
| AI 生成标题 | 正式实现 |
| 多草稿 + 本地自动保存 | 正式实现 |
| 系列/合集 | 正式实现 |
| 灵感模板 | 交互预览已提供 |
| 图片拖拽、封面、九宫格 | 正式实现 |

## 二、AI 改写/润色

### 1. 用户操作

在创建/编辑灵感页：

1. 在正文编辑器里选中一段文字。
2. 点击工具栏「AI 改写」。
3. 选择风格：

```text
简洁
温柔
文艺
干货
活泼
```

4. AI 返回后直接替换选中的正文。

### 2. 后端接口

```text
POST /api/ai/rewrite
```

请求：

```json
{
  "text": "选中的正文",
  "style": "简洁"
}
```

响应：

```json
{
  "code": 200,
  "data": {
    "text": "改写后的正文"
  }
}
```

### 3. 实现原理

```text
浏览器选区
↓
读取选区纯文本
↓
POST /ai/rewrite
↓
DeepSeek 返回 JSON
↓
替换 Range 对应文本
```

后端要求模型只返回：

```json
{"text":"改写后的内容"}
```

### 4. 缓存与限额

- 单次最多 1200 字。
- 缓存 key 包含文本 hash 和风格。
- 同一内容和风格第二次请求可命中 Redis。
- 模型失败时返回原文，不阻断用户继续编辑。

### 5. Token 说明

- 正式调用 AI 改写会消耗 DeepSeek token。
- 命中相同文本和风格的 Redis 缓存时不消耗。
- Playwright 冒烟测试不调用此接口。

## 三、AI 生成标题

### 1. 用户操作

标题输入框右侧增加「AI 生成标题」：

1. 用户写完正文。
2. 点击「AI 生成标题」。
3. 后端一次返回 5 个标题。
4. 用户点击任意标题直接替换。

标题仍受 16 字限制。

### 2. 后端接口

```text
POST /api/ai/titles
```

请求：

```json
{
  "content": "当前灵感正文"
}
```

响应：

```json
{
  "code": 200,
  "data": {
    "titles": [
      "标题候选1",
      "标题候选2",
      "标题候选3",
      "标题候选4",
      "标题候选5"
    ]
  }
}
```

### 3. 实现原理

- 正文最多发送 2000 字。
- 模型返回 JSON 数组。
- 后端使用 `TitleUtil.truncate()` 保证不超过 16 字。
- 自动去重，最多保留 5 个。
- 模型失败时返回一个保底标题。

### 4. Token 说明

- 正式生成标题会调用 DeepSeek。
- 相同正文标题生成结果写入 Redis。
- 命中缓存不再消耗 token。

## 四、多草稿与本地自动保存

### 1. 当前能力

- 最多保留 30 份本地草稿。
- 用户停止输入约 800ms 后自动保存。
- 保存到浏览器 `localStorage`：

```text
inspire:local-drafts:v1
```

- 支持新建、恢复、删除草稿。
- 发布成功或正式保存后删除对应本地草稿。

### 2. 草稿内容

```text
title
tag
content
images
cover
publishCity
updatedAt
```

未上传完成的 `blob:` 图片不会写入本地草稿。

### 3. 为什么先用本地草稿

- 不增加保存接口请求。
- 页面崩溃、误关页面后仍可恢复。
- 不需要等待后端即可保存。
- 适合当前单机预览和快速迭代阶段。

### 4. 后续可扩展

- 增加云端多草稿表。
- 本地草稿与云端草稿合并。
- 记录草稿来源设备。
- 冲突时让用户选择本地版本或云端版本。
- 草稿图片转成持久化地址后同步云端。

## 五、系列/合集

### 1. 正式页面

```text
http://127.0.0.1:5173/#/series/100000000001100000
```

也可从任意一篇属于系列的灵感详情页进入。详情页会在标题/标签下方显示：

```text
系列 · 美食系列 1    1/5
```

并在作者卡片附近提供上一篇、下一篇快捷入口。

### 2. 数据模型

系列主表：

```text
inspire_series
```

字段：

```text
id
user_id
name
description
cover
status
create_time
update_time
deleted
```

系列与灵感的关系：

```text
inspire_main.series_id
inspire_main.series_order
```

其中：

```text
series_id     系列 ID
series_order  系列内阅读顺序，从 1 开始
```

索引：

```text
idx_series_user   (user_id, deleted, update_time)
idx_series_order  (series_id, series_order)
```

### 3. 接口

系列详情：

```text
GET /api/inspire/public/series/{id}
```

返回：

```text
系列名称
系列描述
系列封面
文章总数
按 series_order 排序的文章目录
```

灵感详情接口会补充：

```text
seriesId
seriesName
seriesOrder
seriesTotal
prevSeriesId
prevSeriesTitle
nextSeriesId
nextSeriesTitle
```

### 4. 演示数据

`DemoDataSeeder` 在生成演示数据时会：

1. 按用户最近的公开灵感生成系列。
2. 每 5 篇组成一个系列。
3. 为详情页重点灵感生成一组“深度手记”系列。
4. 回写 `inspire_main.series_id` 和 `inspire_main.series_order`。
5. 排除标题以“冒烟测试”开头的 Playwright 测试灵感。

当前本地演示数据：

```text
系列数：10
已关联灵感：50
```

### 5. 页面行为

- 系列目录页展示封面、系列名称、描述和总篇数。
- 目录按 `series_order` 顺序展示。
- 点击目录项进入对应灵感详情。
- 详情页显示当前篇数和上一篇/下一篇。
- 加载、空、错误状态复用统一 `AppState` 组件。
- `DeviceShell` 会向全局暴露手机屏幕边界，Element Plus 的确认框、对话框、错误弹窗和消息提示统一限制在手机外壳内；所有弹窗由 `styles/overlays.css` 统一为同一套 18px 圆角、标题、按钮、输入框和阴影样式；后台 `deviceShell=false` 时仍保持全屏布局。

### 6. 用户端系列管理

入口：

```text
个人页 → 我的系列
http://127.0.0.1:5173/#/series/manage
```

当前支持：

- 创建系列。
- 修改系列名称和描述。
- 删除系列，文章本身保留并移出系列。
- 从“我的已发布灵感”中搜索并添加文章。
- 将文章移出系列。
- 上下调整文章顺序，顺序写回 `series_order`。
- 查看系列前台页面。
- 管理列表只返回系列摘要；进入具体系列时才加载文章。

管理接口：

```text
GET    /api/inspire/my/series
GET    /api/inspire/my/series/{id}
GET    /api/inspire/my/series/candidates
POST   /api/inspire/series
PUT    /api/inspire/series/{id}
DELETE /api/inspire/series/{id}
POST   /api/inspire/series/{id}/articles
DELETE /api/inspire/series/{id}/articles/{inspireId}
PUT    /api/inspire/series/{id}/order
```

限制：

- 默认单个用户最多创建 50 个系列。
- 默认单个系列最多包含 100 篇灵感。
- 修改、删除、添加和排序都校验系列归属，不能操作其他用户的系列。

### 7. 相关文件

```text
frontend/src/pages/Series.vue
frontend/src/pages/SeriesManage.vue
frontend/src/pages/InspireDetail.vue
frontend/src/router/index.js
frontend/src/api/inspire.js

backend/inspire-core/src/main/java/com/inspire/platform/core/entity/InspireSeries.java
backend/inspire-core/src/main/java/com/inspire/platform/core/mapper/InspireSeriesMapper.java
backend/inspire-core/src/main/java/com/inspire/platform/core/dto/SeriesVO.java
backend/inspire-core/src/main/java/com/inspire/platform/core/service/InspireService.java
backend/inspire-core/src/main/java/com/inspire/platform/core/service/impl/InspireServiceImpl.java
backend/inspire-core/src/main/java/com/inspire/platform/core/controller/InspireController.java
backend/inspire-core/src/main/java/com/inspire/platform/core/seed/DemoDataSeeder.java

docker/init/init.sql
```

## 六、灵感模板预览

### 1. 预览地址

```text
http://127.0.0.1:5190/inspiration-templates-preview.html
```

### 2. 当前模板

| 模板 | 结构重点 |
| --- | --- |
| 探店 | 地点、体验、推荐、价格、避坑 |
| 测评 | 对象、标准、过程、优缺点、结论 |
| 教程 | 材料、步骤、注意事项、完成效果 |
| 清单 | 必备、可选、不建议带、补充说明 |

### 3. 正式实现建议

模板可以配置化：

```text
template_key
name
description
title_example
content_schema
status
```

`content_schema` 存储字段结构，编辑器根据 schema 自动生成引导段落。

模板只提供结构，不自动生成最终内容。

## 七、图片增强

### 1. 拖拽排序

图片卡片支持拖拽：

```text
dragstart
dragover
drop
```

拖放后立即修改 `form.images` 顺序。

### 2. 指定封面

- 图片卡片显示「设为封面」。
- 当前封面有实心边框和「封面」角标。
- 发布时：

```text
payload.img = coverImage
```

不再固定使用第一张图片。

### 3. 九宫格预览

- 顶部「九宫格预览」按钮。
- 按当前顺序展示，封面优先。
- 最多展示 9 张。
- 视频也能出现在九宫格中。
- 预览面板使用页面内固定遮罩，不再使用桌面 `vw` 计算宽度。
- 面板宽度受手机框内容区约束，最大宽度 340px。
- 面板高度受限并支持内部滚动，不会超出页面边界。

### 4. 数据关系

```text
form.images = 多图顺序
form.img = 指定封面
```

后端继续使用：

```text
inspire_main.images
inspire_main.img
```

不需要新增数据库字段。

### 5. 删除按钮修复

原删除标记是普通 `span`，在拖拽卡片和封面按钮层叠时可能无法触发点击。

现已改为：

- 独立 `button` 元素。
- `z-index:8`，始终位于图片内容上方。
- 24×24px 点击区域。
- 阻止拖拽和父卡片点击事件。
- 删除后同步清理：
  - 选中图片
  - 封面图片
  - 视频上传任务
  - 本地预览对象 URL
  - 本地草稿中的自动保存状态

Playwright 已增加验证链路：

```text
上传图片
→ 删除图片
→ 确认列表为空
→ 重新上传
→ 发布灵感
→ 发表评论
```

测试结果：

```text
1 passed
```

## 八、相关文件

### 前端

```text
frontend/src/pages/Create.vue
frontend/src/api/inspire.js
```

### 后端 AI

```text
backend/inspire-ai/src/main/java/com/inspire/platform/ai/controller/AiController.java
backend/inspire-ai/src/main/java/com/inspire/platform/ai/service/AiService.java
backend/inspire-ai/src/main/java/com/inspire/platform/ai/service/impl/AiServiceImpl.java
backend/inspire-ai/src/main/java/com/inspire/platform/ai/dto/AiRewriteRequest.java
backend/inspire-ai/src/main/java/com/inspire/platform/ai/dto/AiRewriteResponse.java
backend/inspire-ai/src/main/java/com/inspire/platform/ai/dto/AiTitleRequest.java
backend/inspire-ai/src/main/java/com/inspire/platform/ai/dto/AiTitleResponse.java
```

### 预览页

```text
projectWord/preview/series-collection-preview.html
projectWord/preview/inspiration-templates-preview.html
```

## 九、验证情况

- 前端构建通过。
- AI 服务编译和打包通过。
- AI 服务已重新部署。
- 系列预览页返回 HTTP 200。
- 模板预览页返回 HTTP 200。
- Playwright 冒烟测试仍通过。

## 十、后续优先级

1. 模板确定预览后，设计后台模板配置与编辑器 schema。
2. 本地草稿增加云端同步。
3. AI 改写支持保留富文本段落结构。
4. AI 标题可增加“更文艺 / 更直接 / 更种草”等标题风格。
