## 搜索服务 — 交付总结

### 搜索模式

| 模式 | 行为 |
|------|------|
| `mysql` | 直接 MySQL LIKE（不需要 ES） |
| `es` | 强制 ES 搜索，ES 不可用时报错 |
| `auto`（默认） | **优先 ES，ES 挂掉自动降级 MySQL** |

配置方式：`application-dev.yml` 里 `inspire.search.mode=auto`

### 模块结构

```
inspire-search (端口 8086)
├── controller/SearchController.java      @Tag + @Operation 中文
├── service/
│   ├── SearchService.java                接口
│   ├── impl/SearchServiceManager.java    路由中心（mode 分发+降级）
│   ├── impl/EsSearchService.java         ES 客户端（懒加载，无 ES 也启动）
│   └── impl/MysqlSearchService.java      MySQL LIKE 降级
├── dto/SearchResultVO.java               @Schema 中文
└── config/SwaggerConfig.java
```

编译通过。现在你的配置是这样的：

**搜索模式：** `auto`（先试 ES → 失败自动降级 MySQL）
**ES 地址：** `localhost:9200`

你只需要把 ES 跑起来，然后创建索引就行：

```bash
# 1. 启动 ES
docker run -d --name elasticsearch \
  -p 9200:9200 -p 9300:9300 \
  -e "discovery.type=single-node" \
  -e "xpack.security.enabled=false" \
  -e "ES_JAVA_OPTS=-Xms512m -Xmx512m" \
  docker.elastic.co/elasticsearch/elasticsearch:7.17.20
```
```bash
# 等几秒确认 ES 就绪
watch -n 1 'curl -s http://localhost:9200 | head -1'
```
```bash
# 2. 创建灵感索引
curl -s -X PUT http://localhost:9200/inspire_index -H "Content-Type: application/json" -d '{
  "settings": {"number_of_shards":1,"number_of_replicas":0},
  "mappings": {
    "properties": {
      "id": {"type":"keyword"},
      "title": {"type":"text","analyzer":"standard","boost":10},
      "tag": {"type":"keyword"},
      "user_id": {"type":"keyword"},
      "view_count": {"type":"long"},
      "like_count": {"type":"integer"},
      "collect_count": {"type":"integer"},
      "heat": {"type":"integer"},
      "publish_city": {"type":"keyword"},
      "create_time": {"type":"date","format":"yyyy-MM-dd HH:mm:ss"},
      "deleted": {"type":"byte"}
    }
  }
}'
```

然后启动搜索服务并测试：

```bash
# 终端
cd "/Users/lee/Desktop/Project/Study Project/Codex/inspire-ai-preview/backend"
mvn spring-boot:run -pl inspire-search

# 另一个终端测试
# 先创建一条数据到 ES（手动同步）
ID=$(curl -s "http://localhost:8083/inspire/public/list?page=1&size=1" | \
  python3 -c "import sys,json;d=json.load(sys.stdin)['data'];print(d[0]['id'] if d else '')")

# 把 MySQL 数据同步到 ES（手动索引一条）
curl -s -X PUT "http://localhost:9200/inspire_index/_doc/$ID" -H "Content-Type: application/json" \
  -d "{\"id\":$ID,\"title\":\"鸡腿的五种神仙吃法\",\"tag\":\"美食\",\"heat\":0,\"view_count\":0,\"like_count\":0,\"collect_count\":0,\"publish_city\":\"长沙\",\"deleted\":0}"

# 搜索
curl -s --get "http://localhost:8080/api/search/public" --data-urlencode "keyword=鸡腿"
```

如果搜索结果的 `source` 字段是 `"es"`，说明 ES 模式生效了。如果是 `"mysql"`，说明自动降级到了 MySQL。

需要我解释下 ES 的思路，或者下一步直接做**管理员后台**？


### 接口

| 方法 | 路径 | 说明 | 白名单 |
|------|------|------|--------|
| GET | `/api/search/public?keyword=xxx` | 搜索灵感 | ✅ |

**Swagger：** `http://localhost:8086/swagger-ui/index.html`

### 测试
```bash
# === 终端 1：网关 ===
cd "/Users/lee/Desktop/Project/Study Project/Codex/inspire-ai-preview/backend"
mvn spring-boot:run -pl inspire-gateway
```
```bash
# === 终端 2：用户服务 ===
cd "/Users/lee/Desktop/Project/Study Project/Codex/inspire-ai-preview/backend"
mvn spring-boot:run -pl inspire-auth
```
```bash
# === 终端 3：灵感核心 + 搜索 ===
mvn spring-boot:run -pl inspire-core
```
```bash
mvn spring-boot:run -pl inspire-ai     # 可选
```
```bash
mvn spring-boot:run -pl inspire-search  # 可选
```

```bash
# MySQL 模式搜索（不需要 ES）
curl -s --get "http://localhost:8080/api/search/public" \
  --data-urlencode "keyword=鸡腿"
```

```bash
# 带分类筛选
curl -s --get "http://localhost:8080/api/search/public" \
  --data-urlencode "keyword=鸡腿" \
  --data-urlencode "tag=美食"
```

```bash
# 注册 → 登录 → 创建灵感 → 搜索 — 一条龙
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/register \
-H "Content-Type: application/json" \
-d '{"username":"alice","password":"123456","confirmPassword":"123456","email":"alice@test.com"}' \
| python3 -c "import sys,json;print(json.load(sys.stdin).get('data',{}).get('token',''))")

[ -n "$TOKEN" ] && echo "✅ 注册登录成功" || echo "⚠️ 用户已存在，尝试登录"
[ -z "$TOKEN" ] && TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
-H "Content-Type: application/json" \
-d '{"username":"alice","password":"123456"}' \
| python3 -c "import sys,json;print(json.load(sys.stdin)['data']['token'])")

curl -s -X POST http://localhost:8080/api/inspire \
-H "Authorization: Bearer $TOKEN" \
-H "Content-Type: application/json" \
-d '{"title":"鸡腿的五种做法","content":"详细步骤...","tag":"美食","status":1}' \
| python3 -c "import sys,json;print('✅ 创建灵感成功' if json.load(sys.stdin)['code']==200 else '❌ '+str(json.load(sys.stdin)))"

curl -s --get "http://localhost:8080/api/search/public" --data-urlencode "keyword=鸡腿" \
| python3 -c "import sys,json;d=json.load(sys.stdin);print(f'✅ 搜索到 {len(d.get(\"data\",[]))} 条结果')"
```


---

目前后端 6 个模块全部就位：

```
inspire-common  →  公共工具 + 统一返回
inspire-gateway →  网关（8080）
inspire-auth    →  用户服务（8081）
inspire-ai      →  AI 创作（8082）
inspire-core    →  灵感核心（8083）
inspire-search  →  搜索（8086）
```

接下来想继续走哪个方向？
1. **管理员后台**（监控大屏 + 灵感管理 + 推送配置）
2. **前端的 AI 创作页面**联调（把灵感创作 + 灵感展示接上后端）
3. **其他中间件对接**（RocketMQ / Flink）





