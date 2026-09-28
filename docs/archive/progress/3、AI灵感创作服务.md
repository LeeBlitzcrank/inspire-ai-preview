## AI 灵感创作服务 — 已交付

### 模块结构

```
inspire-ai/
├── pom.xml                              # 依赖：web + validation + swagger + common
├── src/main/java/com/inspire/platform/ai/
│   ├── InspireAiApplication.java        # 启动类，端口 8082
│   ├── config/SwaggerConfig.java        # 中文 API 文档
│   ├── controller/AiController.java     # 3 个端点 + @Operation 中文描述
│   ├── service/
│   │   ├── AiService.java               # 接口
│   │   └── impl/AiServiceImpl.java      # 模拟 AI 实现（10 类知识库 + 模糊 + 动态生成）
│   └── dto/
│       ├── AiGenerateRequest.java       # 关键词 + 城市
│       ├── AiGenerateResponse.java      # 2 条灵感候选
│       ├── InspirationCandidate.java    # 单条灵感卡片（id / title / summary / tag）
│       ├── AiSelectRequest.java         # 选中灵感（上报行为数据）
│       └── AiPublishRequest.java        # 发布灵感
└── src/main/resources/application.yml   # mock-enabled: true
```

### 三个接口

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/ai/generate` | 输入关键词 → 返回 2 条灵感候选 |
| POST | `/api/ai/select` | 用户选中一条，上报行为日志 |
| POST | `/api/ai/publish` | 发布完整灵感至广场 |

### 模拟 AI 策略（三级回退）

```
keyword=鸡腿 → 精确匹配 → 返回"鸡腿的五种神仙吃法" / "减脂期也能吃的鸡腿做法"
keyword=运动 → 精确匹配 → 返回"15分钟居家燃脂计划" / "新手跑步入门指南"
keyword=跑   → 模糊匹配 → 命中"跑步"+"运动"的知识库
keyword=无人机 → 动态生成 → "无人机创意拓展方案" / "无人机进阶技巧合集"
```

内置 10 个分类知识库：鸡腿、运动、跑步、健身、穿搭、电影、文案、美食、旅游、摄影

### 测试方法

```bash
# 先启动三个服务（三个终端）
cd "/Users/lee/Desktop/Project/Study Project/Codex/inspire-ai-preview/backend"
mvn spring-boot:run -pl inspire-auth
mvn spring-boot:run -pl inspire-gateway
mvn spring-boot:run -pl inspire-ai
```

```bash
curl -s -X POST http://localhost:8080/api/auth/register \
-H "Content-Type: application/json" \
-d '{"username":"testai","password":"123456","confirmPassword":"123456","email":"testai@test.com"}'
```

```bash
# 1. 先登录获取 token
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"testai","password":"123456"}' | python3 -c "import sys,json; print(json.load(sys.stdin)['data']['token'])")
echo "TOKEN=$TOKEN"
```

```bash
# 2. AI 生成灵感
curl -s -X POST http://localhost:8080/api/ai/generate \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"keyword":"鸡腿","city":"长沙"}'
```

```bash
# 3. 选中一条灵感
curl -s -X POST http://localhost:8080/api/ai/select \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"keyword":"鸡腿","selectedId":1,"selectedTitle":"鸡腿的五种神仙吃法","city":"长沙"}'
```

```bash
# 4. 发布灵感
curl -s -X POST http://localhost:8080/api/ai/publish \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"title":"鸡腿的五种神仙吃法","content":"详细做法步骤...","tag":"美食","city":"长沙"}'
```

```bash
# Swagger 文档
open http://localhost:8082/swagger-ui/index.html
```

下一个模块想做什么？按排期表，接下来有几个方向：
1. **灵感核心服务**（inspire-main/inspire-content CRUD + 分页列表）
2. **管理员后台**（监控大屏、灵感管理、推送配置）
3. 或者先把**前端的 AI 创作页面**和这些后端接口联调起来


目前是**模拟模式**——内置了一个关键词→灵感的映射表，对输入的 `鸡腿`、`运动`、`穿搭` 等关键词做了精确和模糊匹配，匹配不到的会动态拼接模板文字。

### 如果想接入真实 AI，可以选一个方式

**方式一：OpenAI 兼容接口**
调用 OpenAI（或任何兼容的 API），把关键词发给大模型生成灵感。改动成本小，效果最好。

**方式二：本地模型**
用 Ollama 跑个开源模型（比如 qwen2 或 llama3），本地调 API，不需要外网。


```
AiController → AiService (接口) ← AiServiceImpl (当前模拟实现)
                                  ← (以后) OpenAiServiceImpl (真实实现)
```

控制器只依赖 `AiService` 接口，只需要：

1. **新增一个实现类** `OpenAiServiceImpl`，里面调 OpenAI/Codex 的 API
2. **加一个开关配置**，`application.yml` 里从 `mock: true` 改成 `mock: false`
3. **可能加一个 HTTP 客户端依赖**（`spring-boot-starter-webflux` 或 `RestTemplate`）

