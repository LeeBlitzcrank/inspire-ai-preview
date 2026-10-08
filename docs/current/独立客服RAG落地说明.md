# 独立客服 RAG 落地说明

> 更新时间：2026-10-09  
> 状态：已实现，包含客服问答、转人工工单和报错截图/视频附件。

## 1. 目标

让用户可以直接询问“项目怎么使用”或“项目技术怎么实现”，例如：

- 怎么发布灵感
- 怎么绑定手机号
- 世界种子怎么玩
- 收藏夹怎么管理
- 某个页面为什么没有数据
- 鉴权和网关是怎么实现的
- 数据库和 RAG 索引怎么设计

客服回答只依据项目文档，不混入用户灵感内容，不读取密钥和内部运维信息。

## 2. 为什么独立

原有 `inspire_rag_index` 用于回答“有哪些灵感”，数据来源是用户发布的灵感。

客服 RAG 需要回答“这个网站怎么用”，数据来源是项目说明文档。两者混在一起会导致：

- 用户灵感污染产品说明。
- 产品文档被当作灵感推荐。
- 无法单独重建或关闭客服知识库。

因此新增独立索引：

```text
support_knowledge_index
```

## 3. 数据来源

客服只读取一份面向客户维护的文档：

```text
docs/current/项目客服知识库.md
```

该文档包含：

- 项目定位和核心功能。
- 用户操作流程。
- 技术架构和实现概览。
- 常见问题解答。

开发审计、密钥清理、生产运维命令、压测和临时变更文档不会进入客服知识库。

明确排除：

```text
docs/archive/**
.env
密钥、Token、授权码
内部数据库和服务器信息
管理员审计信息
```

## 4. 处理链路

```text
Markdown 文件
  -> 标题层级切片
  -> 每片约 900 字，重叠 120 字
  -> Ollama embeddinggemma:300m
  -> Elasticsearch support_knowledge_index

用户问题
  -> 向量召回
  -> 标题/章节/正文关键词召回
  -> RRF 融合
  -> 默认文档摘要；可选 Ollama deepseek-r1:8b 或 DeepSeek
  -> 答案 + 文档来源
```

## 5. 接口

公开接口：

```text
POST /api/rag/support/ask
POST /api/rag/support/search
GET  /api/rag/support/status
POST /api/rag/support/handoff
GET  /api/rag/support/ticket/{ticketNo}
POST /api/file/support/upload
```

请求示例：

```json
{
  "query": "怎么发布一篇灵感？",
  "topK": 6
}
```

内部重建接口：

```text
POST /rag/support/admin/reindex
Header: X-Rag-Token: <INSPIRE_RAG_ADMIN_TOKEN>
```

该内部接口不经过网关暴露。

转人工工单接口：

- 匿名或登录用户都可以提交工单。
- 匿名用户创建成功后会返回 `accessToken`，用于查看自己的工单和回复。
- 单个工单最多附带 5 个截图或视频。
- 视频限制沿用文件服务规则，单个最大 50MB。

## 6. 前端入口

全站右下角增加“客服”浮动按钮，后台管理页不显示。

功能：

- 推荐问题
- 多轮当前会话展示
- 文档来源展示
- 点击来源复制文件路径和章节
- 输入“人工”“真人”“转人工”等关键词后打开人工工单提交面板
- 支持填写联系方式、问题描述和上传截图/视频
- 右上角“工单”入口可查看历史工单和人工回复
- 移动端自适应

主要文件：

```text
frontend/src/components/SupportAssistant.vue
frontend/src/api/support.js
```

## 7. 配置

```text
INSPIRE_SUPPORT_RAG_ENABLED=true
INSPIRE_SUPPORT_RAG_INDEX_NAME=support_knowledge_index
INSPIRE_SUPPORT_RAG_ROOT_DIR=/knowledge
INSPIRE_SUPPORT_RAG_DEFAULT_TOP_K=6
INSPIRE_SUPPORT_RAG_CANDIDATE_K=30
INSPIRE_SUPPORT_RAG_SCAN_DELAY_MS=600000
INSPIRE_SUPPORT_RAG_ANSWER_PROVIDER=fallback
INSPIRE_SUPPORT_RAG_OLLAMA_MODEL=deepseek-r1:8b
INSPIRE_SUPPORT_UPLOAD_RATE_LIMIT_REPLENISH=1
INSPIRE_SUPPORT_UPLOAD_RATE_LIMIT_BURST=3
```

`embeddinggemma:300m` 继续复用现有 RAG 的 Ollama 配置。

默认 `fallback` 会先判断问题是操作类还是技术类，再基于 Top 3 文档整理成可读说明，不会直接把整段原文无结构堆给用户，实测约 200-700ms。  
本机纯 CPU 的 `deepseek-r1:8b` 最小生成也超过 180 秒，因此只作为可选模式，不建议直接用于在线客服。配置 DeepSeek API 后可切换为 `deepseek`。

## 8. 重建索引

全量重建：

```bash
bash scripts/support-rag-reindex.sh
```

查看状态：

```bash
curl http://127.0.0.1:8087/rag/support/status
```

服务每 10 分钟检查文档指纹。文档新增、修改或删除后，会自动重建客服索引。

## 9. 安全与限制

- 系统提示词要求只依据提供的项目文档回答。
- 无法确认时明确回答“文档中没有明确说明”。
- 不返回密钥、数据库、内部地址和管理员信息。
- 单个问题最大 500 字。
- 单次最多返回 10 条来源，默认 6 条。
- 网关对匿名客服接口按 IP 限流。
- 客服附件上传使用独立 IP 限流，不复用登录后的通用上传额度。
- 工单只接受本站上传地址，避免被写入任意外链。
- 回答失败时回退为文档摘要，不依赖模型也能给出可追溯内容。

## 10. 后续扩展

- 增加多轮会话记忆和会话摘要。
- 增加回答有用/无用反馈。
- 增加流式 SSE 输出。
- 增加文档版本和生效时间。
- 对高频问题预生成标准答案。
- 增加邮件、短信或站内通知，提醒用户人工已回复。
- 将人工工单升级为 WebSocket/SSE 实时客服工作台。
