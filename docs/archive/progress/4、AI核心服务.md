
## 灵感核心服务 — 完整交付

### 模块结构

```
inspire-core (端口 8083)
├── config/        MyBatisPlusConfig（动态分表拦截器）、ShardContext、SwaggerConfig
├── entity/        InspireMain、InspireContent、CollectAction、LikeAction
├── mapper/        InspireMainMapper、InspireContentMapper、CollectMapper、LikeMapper
├── dto/           InspireCreateRequest、InspireUpdateRequest、InspirePageQuery、InspireVO
├── service/       InspireService + InspireServiceImpl
├── controller/    InspireController（11 个端点）
└── resources/     schema.sql（自动建表）、application.yml/dev.yml
```

### 接口清单

| 方法 | 路径 | 说明 | 白名单 |
|------|------|------|--------|
| GET | `/inspire/public/list` | 公开列表（分页+分类+排序） | ✅ |
| GET | `/inspire/public/{id}` | 灵感详情（含正文+收藏点赞状态） | ✅ |
| GET | `/inspire/my` | 我的发布 | |
| GET | `/inspire/my/drafts` | 我的草稿 | |
| POST | `/inspire` | 创建（status=0草稿/1发布） | |
| PUT | `/inspire/{id}` | 修改 | |
| DELETE | `/inspire/{id}` | 删除（本人限） | |
| POST | `/inspire/{id}/collect` | 收藏（分表 `user_id%10`） | |
| DELETE | `/inspire/{id}/collect` | 取消收藏 | |
| POST | `/inspire/{id}/like` | 点赞（分表 `inspire_id%10`） | |
| DELETE | `/inspire/{id}/like` | 取消点赞 | |

### 数据库表

`schema.sql` 首次启动自动建：
- `inspire_main` — 灵感主表（含分类、热度、计数）
- `inspire_content` — 正文附表（冷热分离）
- `collect_0~9` — 10 张收藏分表（`user_id%10` 路由，联合唯一索引）
- `inspire_like_0~9` — 10 张点赞分表（`inspire_id%10` 路由，联合唯一索引）

### 测试方法

```bash
# 启动四个服务
cd "/Users/lee/Desktop/Project/Study Project/Codex/inspire-ai-preview/backend"
mvn spring-boot:run -pl inspire-gateway  # 8080
mvn spring-boot:run -pl inspire-auth     # 8081
mvn spring-boot:run -pl inspire-core     # 8083
```
```bash
# 注册并登录
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"coretest1","password":"123456","confirmPassword":"123456","email":"core1@test.com"}' \
  | python3 -c "import sys,json;print(json.load(sys.stdin)['data']['token'])")
```
```bash
# 创建灵感
curl -s -X POST http://localhost:8080/api/inspire \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"title":"鸡腿的五种神仙吃法","content":"详细步骤...","tag":"美食","status":1,"publishCity":"长沙"}'
```
```bash
# 公开列表（不用 token）
curl -s "http://localhost:8080/api/inspire/public/list?tag=美食&sort=heat&page=1&size=10"
```
```bash
# 公开详情（不用 token）
curl -s http://localhost:8080/api/inspire/public/{197197854379347969}
```
```bash
# 收藏
curl -s -X POST http://localhost:8080/api/inspire/{curl -s http://localhost:8080/api/inspire/public/{197197854379347969}}/collect \
  -H "Authorization: Bearer $TOKEN"
```
```bash
# 点赞
curl -s -X POST http://localhost:8080/api/inspire/{197197854379347969}/like \
  -H "Authorization: Bearer $TOKEN"
```

/*****
```bash
# 收藏 + 点赞
curl -s -X POST http://localhost:8080/api/inspire/{197197854379347969}/collect -H "Authorization: Bearer $TOKEN"
```

```bash
curl -s -X POST http://localhost:8080/api/inspire/{197197854379347969}/like -H "Authorization: Bearer $TOKEN"
```
****/
---

现在后端已经搭了 5 个模块：

```
inspire-backend
├── inspire-common    公共工具
├── inspire-gateway   网关（8080）
├── inspire-auth      用户服务（8081）
├── inspire-ai        AI创作（8082）
└── inspire-core      灵感核心（8083）
```

接下来想做什么？可以继续推进的方向：
1. **管理员后台服务**（监控大屏、灵感管理、推送配置）
2. **搜索服务**（ES 对接，或者先用 MySQL LIKE）
3. **推荐服务**（热点排序、个性化推荐）
4. **前端联调**（把现有前端页面和后端接起来）