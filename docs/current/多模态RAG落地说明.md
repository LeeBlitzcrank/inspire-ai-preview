# 多模态 RAG 落地说明

> 更新时间：2026-09-28
> 状态：MVP 已实现，本地已启用 Ollama 真实语义向量

## 1. 目标

在现有灵感平台上增加一个可运行的多模态 RAG：

- 用自然语言提问全站灵感。
- 上传图片，按视觉特征检索相似灵感。
- 返回带来源链接的回答，不直接让模型凭空生成。
- 发布、编辑、删除灵感后异步更新索引，不阻塞主业务。
- 默认不依赖付费向量数据库；Ollama 和大模型能力均可选。

## 2. 服务结构

新增独立服务 `inspire-rag`，端口 `8087`。

```text
浏览器
  -> Spring Cloud Gateway
  -> /api/rag/ask | /api/rag/search | /api/rag/status
  -> inspire-rag
      -> MySQL（灵感源数据 + RAG 状态）
      -> Elasticsearch（关键词 + dense_vector 向量检索）
      -> RocketMQ（发布/编辑/删除异步同步）
      -> DeepSeek（可选，基于检索结果生成回答）
      -> Ollama（本地 embeddinggemma:300m，可选 bge-m3 / Qwen2.5-VL）
```

`GET /rag/admin/reindex` 对应的内部接口不经过网关，只绑定在本机 `127.0.0.1:8087`。

## 3. 索引内容

每条已发布灵感会生成两类文档：

### 文本 chunk

```text
标题
分类
城市
系列
正文
```

正文按约 700 字切块，块间保留 80 字重叠。

### 图片 chunk

每篇最多索引 3 张图片。图片描述按以下顺序生成：

1. 启用 Ollama 视觉模型时，由 `Qwen2.5-VL` 生成中文图片描述。
2. 未启用模型时，使用 Java `ImageIO` 提取基础视觉特征：
   - 暖色 / 冷色 / 自然中性色
   - 低饱和 / 高饱和
   - 暗调 / 中间调 / 明亮
   - 横图 / 竖图 / 方图
3. 图片无法读取时，退回标题和分类。

这保证默认部署不需要下载大模型，也能完成基础多模态检索。

## 4. 检索流程

```text
用户问题 / 图片
  -> 图片描述（可选）
  -> embedding
  -> ES 向量召回 Top 30
  -> ES 关键词召回 Top 30
  -> RRF 融合去重
  -> 选取 Top 8
  -> DeepSeek 生成带引用回答
```

向量检索当前使用 Elasticsearch `dense_vector` + 精确 cosine 检索，适合当前万级以下数据。数据进入百万级后，建议把 `ElasticsearchRagStore` 替换为 Qdrant 实现，接口保持不变。

服务启动时会校验索引 mapping。如果发现 `embedding` 不是 `dense_vector`，或向量维度与当前模型不一致，会自动删除并重建索引，避免切换 embedding 模型后出现维度冲突。

如果 ES 索引被删除或重建，而 MySQL 仍保留旧的索引状态，定时任务会检测 `elasticsearchDocuments=0`，清理状态并自动重新灌入数据，不需要手工清库。

## 5. Embedding

当前本地启用配置：

```text
INSPIRE_RAG_OLLAMA_EMBEDDING_ENABLED=true
INSPIRE_RAG_EMBEDDING_MODEL=embeddinggemma:300m
INSPIRE_RAG_EMBEDDING_DIMS=768
INSPIRE_RAG_EMBEDDING_BATCH_SIZE=32
```

`embeddinggemma:300m` 是 Ollama 官方多语言语义向量模型，CPU 下明显快于 `bge-m3`，768 维向量与当前 ES mapping 一致。

安装并启动：

```bash
ollama pull embeddinggemma:300m
ollama serve
```

如果仍希望使用质量更高但更慢的 `bge-m3`，可切换为：

```text
INSPIRE_RAG_OLLAMA_EMBEDDING_ENABLED=true
INSPIRE_RAG_EMBEDDING_MODEL=bge-m3
INSPIRE_RAG_EMBEDDING_DIMS=1024
INSPIRE_RAG_OLLAMA_URL=http://host.docker.internal:11434
```

切换 embedding 模型或维度后必须全量重建 `inspire_rag_index`。服务会自动识别 mapping 维度变化并重建 ES 索引，不会把 768 维和 1024 维向量混在同一索引中。

真实语义模式下，索引阶段不允许静默降级为本地哈希向量；Ollama 批量失败会保留旧索引并重试。查询阶段如果 Ollama 临时不可用，则自动退化为关键词检索，避免使用不兼容的查询向量。

## 6. 图片问答

上传图片时，RAG 会先提取图片描述，再把“用户文字 + 图片描述”作为检索查询。

如需更高质量的视觉理解：

```bash
ollama pull qwen2.5vl:7b
```

配置：

```text
INSPIRE_RAG_OLLAMA_CAPTION_ENABLED=true
INSPIRE_RAG_CAPTION_MODEL=qwen2.5vl:7b
```

该模型会提高索引阶段的图片描述质量，也会增加索引耗时。

## 7. DeepSeek 回答

回答模型复用现有：

```text
INSPIRE_DEEPSEEK_API_KEY
INSPIRE_DEEPSEEK_API_URL
```

没有 key 或 DeepSeek 调用失败时，系统不会失败，而是返回检索结果摘要和来源列表。

## 8. 数据表

Flyway `V6__multimodal_rag.sql` 新增：

### `rag_index_state`

| 字段 | 说明 |
| --- | --- |
| `inspire_id` | 灵感 ID |
| `content_hash` | 源内容摘要 |
| `chunk_count` | 索引 chunk 数量 |
| `source_update_time` | 源记录更新时间 |
| `indexed_at` | 最后成功索引时间 |
| `error_message` | 最近一次失败原因 |

### `rag_query_log`

记录问题、模式、TopK、命中数和耗时，用于后续分析搜索需求和调优。

## 9. 接口

### 问答

```http
POST /api/rag/ask
Authorization: Bearer <accessToken>
Content-Type: application/json

{
  "query": "找一些低饱和、有生活感的露营灵感",
  "imageBase64": "data:image/jpeg;base64,...",
  "topK": 8
}
```

### 仅检索

```http
POST /api/rag/search
```

### 状态

```http
GET /api/rag/status
```

### 内部全量/批量重建

该接口不经过网关，仅本机访问：

```bash
curl -X POST 'http://127.0.0.1:8087/rag/admin/reindex?limit=1000'
```

如果设置了 `INSPIRE_RAG_ADMIN_TOKEN`，请求需要带：

```text
X-Rag-Token: <token>
```

### 内部重建进度

```http
GET /rag/admin/reindex/status
X-Rag-Token: <token>
```

返回 `phase`、`totalDocuments`、`processedDocuments`、`totalChunks`、`processedChunks`、`progressPercent`、`elapsedMs`、`estimatedRemainingMs` 和错误信息。

本地推荐直接使用：

```bash
bash scripts/rag-reindex.sh 1200
```

脚本会持续输出阶段、已完成 chunk、百分比和预计剩余时间。

## 10. 索引同步

### 实时同步

发布、编辑或删除灵感时，`inspire-core` 向 `topic_inspire_rag_sync` 发送事件：

```json
{
  "inspireId": 100000000000100222,
  "operation": "UPSERT"
}
```

删除事件使用 `DELETE`。

### 补偿同步

`inspire-rag` 每 30 秒扫描：

```text
rag_index_state 不存在
或 source_update_time 早于 inspire_main.update_time
```

每次最多处理 `INSPIRE_RAG_SYNC_BATCH_SIZE=50` 条，避免抢占主服务资源。

## 11. 安全

- `/api/rag/ask`、`/api/rag/search`、`/api/rag/status` 需要登录。
- 内部重建接口不通过网关暴露。
- 只索引 `status=1 AND deleted=0` 的公开内容。
- RAG 回答只使用检索到的上下文，并提示模型不得编造。
- 查询日志最多保存 500 字，不记录上传图片完整数据。
- 生产环境建议设置 `INSPIRE_RAG_ADMIN_TOKEN`。

## 12. 启动与验证

重新构建并启动：

```bash
./scripts/start.sh
```

检查状态：

```bash
curl http://127.0.0.1:8087/rag/status
```

触发第一批索引：

```bash
bash scripts/rag-reindex.sh 1200
```

预期返回：

```json
{
  "code": 200,
  "data": {
    "indexed": 1004,
    "indexedTotal": 1004
  }
}
```

## 13. 容量与扩展

当前实现适合：

- 本地开发
- 万级灵感
- 十万级 chunk
- 单机 ES 精确向量检索

扩展到百万/亿级时：

1. 把 embedding 改为独立 GPU/CPU 服务并从主服务解耦。
2. 将 ES 向量检索替换为 Qdrant/Milvus。
3. 引入 `bge-reranker-v2-m3` 精排。
4. 图片直接使用 Chinese-CLIP，避免只依赖描述文本。
5. 视频增加 Whisper 字幕和关键帧向量。
6. 使用独立 topic 和消费者组按优先级索引，避免热点内容拖慢普通内容。

## 14. 本次实测

当前本地种子库：

```text
公开灵感：1004
成功索引灵感：1004
ES 文档：3011（文本 chunk + 图片 chunk）
ES 向量字段：dense_vector / 768 dims
Embedding provider：ollama:embeddinggemma:300m
```

性能对比（同样 32 条真实中文内容，CPU）：

```text
embeddinggemma:300m：4.17s，768 dims，语义相关/无关差距 0.2945
bge-m3：7.43s，1024 dims，语义相关/无关差距 0.2895
```

全量重建实测：

```text
阶段：completed
成功索引：1004 / 1004
ES 文档：3011
总耗时：约 9 分 51 秒
降级/失败：0
```

检索测试：

```text
POST /api/rag/search {"query":"安静温暖，适合一个人周末独处的地方","topK":5}
网关状态：200
provider：ollama:embeddinggemma:300m
返回来源：5 条
耗时：约 473ms
```
