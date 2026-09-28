# Elasticsearch 常用操作命令（curl 原生，适配你 7.17 单节点）
## 前置
ES 地址：`http://127.0.0.1:9200`

## 一、集群基础信息
### 1. 查看集群健康状态
```bash
curl http://127.0.0.1:9200/_cat/health?v
# 简洁输出
curl -s http://127.0.0.1:9200/_cluster/health | jq
```
status：`green`正常 / `yellow`副本缺失 / `red`分片丢失

### 2. 查看集群节点
```bash
curl http://127.0.0.1:9200/_cat/nodes?v
```

### 3. 查看ES版本、基础信息
```bash
curl http://127.0.0.1:9200
```

## 二、索引操作（最常用）
### 1. 查看所有索引
```bash
curl http://127.0.0.1:9200/_cat/indices?v
```

### 2. 创建索引（指定分片、映射示例）
```bash
curl -XPUT http://127.0.0.1:9200/article -H "Content-Type:application/json" -d '
{
  "settings": {
    "number_of_shards": 1,
    "number_of_replicas": 0
  },
  "mappings": {
    "properties": {
      "id": {"type":"long"},
      "title": {"type":"text"},
      "content": {"type":"text"},
      "createTime": {"type":"date"}
    }
  }
}'
```

### 3. 查看索引结构/mapping
```bash
curl http://127.0.0.1:9200/article/_mapping
```

### 4. 删除索引（谨慎！）
```bash
curl -XDELETE http://127.0.0.1:9200/article
```

## 三、文档CRUD
### 1. 新增文档（指定id）
```bash
curl -XPOST http://127.0.0.1:9200/article/_doc/1 -H "Content-Type:application/json" -d '
{
  "id": 1,
  "title": "ES入门",
  "content": "Elasticsearch 全文检索",
  "createTime": "2026-06-29"
}'
```

### 2. 查询单条文档
```bash
curl http://127.0.0.1:9200/article/_doc/1
```

### 3. 全量修改文档
```bash
curl -XPUT http://127.0.0.1:9200/article/_doc/1 -H "Content-Type:application/json" -d '
{
  "id":1,
  "title":"ES完整教程",
  "content":"全文检索引擎",
  "createTime":"2026-06-29"
}'
```

### 4. 局部更新文档
```bash
curl -XPOST http://127.0.0.1:9200/article/_doc/1/_update -H "Content-Type:application/json" -d '
{
  "doc": {
    "title": "ES局部更新"
  }
}'
```

### 5. 删除文档
```bash
curl -XDELETE http://127.0.0.1:9200/article/_doc/1
```

## 四、检索查询 DSL
### 1. 查询索引全部数据
```bash
curl -XPOST http://127.0.0.1:9200/article/_search -H "Content-Type:application/json" -d '
{
  "query": {
    "match_all": {}
  }
}'
```

### 2. 关键词全文检索（match）
```bash
curl -XPOST http://127.0.0.1:9200/article/_search -H "Content-Type:application/json" -d '
{
  "query": {
    "match": {
      "content": "检索"
    }
  }
}'
```

### 3. 精确匹配 term（不分词）
```bash
curl -XPOST http://127.0.0.1:9200/article/_search -H "Content-Type:application/json" -d '
{
  "query": {
    "term": {
      "id": 1
    }
  }
}'
```

### 4. 分页 + 排序
```bash
curl -XPOST http://127.0.0.1:9200/article/_search -H "Content-Type:application/json" -d '
{
  "from": 0,
  "size": 10,
  "sort": [{"createTime":"desc"}],
  "query": {"match_all": {}}
}'
```

## 五、批量操作 bulk
### 批量新增文档
```bash
curl -XPOST http://127.0.0.1:9200/article/_bulk -H "Content-Type:application/json" -d '
{"index":{"_id":2}}
{"id":2,"title":"批量1","content":"批量数据"}
{"index":{"_id":3}}
{"id":3,"title":"批量2","content":"多条写入"}
'
```

## 六、分词测试
```bash
curl -XPOST http://127.0.0.1:9200/_analyze -H "Content-Type:application/json" -d '
{
  "text": "Elasticsearch 全文检索引擎"
}'
```

## 七、容器内ES相关命令（docker）
```bash
# 查看ES实时日志
docker logs -f elasticsearch

# 进入ES容器终端
docker exec -it elasticsearch /bin/bash

# 重启ES容器
docker restart elasticsearch

# 删除重建ES
docker rm -f elasticsearch
```

## 八、Mac 实时监控ES健康（替代watch）
```zsh
# 每秒刷新集群状态
while true; do clear; curl -s http://127.0.0.1:9200/_cluster/health | jq; sleep 1; done
```
安装格式化工具 `brew install jq`


# 删掉旧的脏数据
docker exec -it $(docker ps -q --filter name=mysql) mysql -uroot -p123456 inspire_ai_preview \
-e "SELECT * FROM collect_3;"


# 1. 确认你现在的 token
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
-H "Content-Type: application/json" \
-d '{"username":"admin","password":"123456"}' \
| python3 -c "import sys,json;print(json.load(sys.stdin)['data']['token'])")

# 2. 收藏一条灵感（把下面的 id 换成列表里返回的任何一个）
curl -s -X POST "http://localhost:8080/api/inspire/197219830972026881/collect" \
-H "Authorization: Bearer $TOKEN"

# 3. 查收藏列表
curl -s -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/inspire/my/collects