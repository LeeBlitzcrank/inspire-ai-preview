# 灵思集 管理员后台使用指南

## 一、访问入口

| 环境 | 地址 |
|------|------|
| 本地开发 | `http://localhost:5173/admin/login` |

---

## 二、管理员登录

### 2.1 默认账号

| 账号 | 密码 | 说明 |
|------|------|------|
| `admin` | `admin123` | 超级管理员 |

首次启动时系统自动创建，如需新增管理员，直接插入 `admin_user` 表：

```sql
INSERT INTO admin_user(username, password, nickname)
VALUES('newadmin', '$2a$10$...BCrypt哈希...', '新管理员');
```

### 2.2 登录步骤

1. 浏览器打开 `http://localhost:5173/admin/login`
2. 输入账号 `admin`、密码 `admin123`
3. 点击「登录」，进入管理后台

---

## 三、监控大屏

**路径：** 侧边栏 → 监控大屏
**接口：** `GET /api/admin/dashboard`

展示数据：

| 指标 | 来源 | 说明 |
|------|------|------|
| 灵感总数 | `inspire_main` 表 | 已发布的灵感数量 |
| 用户总数 | `user` 表 | 注册用户总数 |
| AI 调用次数 | — | 暂未接入，预留 |
| 分类热度 | `inspire_main` GROUP BY tag | 各分类灵感数量 |
| 城市分布 | `inspire_main` GROUP BY publish_city | 各城市灵感发布量 |

---

## 四、灵感管理

**路径：** 侧边栏 → 灵感管理

### 4.1 搜索筛选

| 筛选条件 | 说明 |
|----------|------|
| 标题搜索 | 按标题关键词模糊搜索 |
| 分类筛选 | 美食/运动/电影/穿搭/文案/旅游/摄影/其他 |
| 状态筛选 | 已发布 / 草稿 |

### 4.2 下架/上架操作

**接口：**
- 下架：`PUT /api/admin/inspire/{id}/block`
- 上架：`PUT /api/admin/inspire/{id}/unblock`

**规则：**
- 下架灵感 → `deleted` 字段设为 `2` → 不再参与推荐与检索
- 上架灵感 → `deleted` 字段恢复为 `0` → 正常展示
- 灵感不存在时返回 `{"code":500, "msg":"灵感不存在，ID=xxx"}`

**curl 测试：**

```bash
ADMIN_TOKEN="上一步登录返回的token"

curl -s -X PUT -H "Authorization: Bearer $ADMIN_TOKEN" \
  "http://localhost:8085/admin/inspire/197219830972026881/block"

curl -s -X PUT -H "Authorization: Bearer $ADMIN_TOKEN" \
  "http://localhost:8085/admin/inspire/197219830972026881/unblock"
```

---

## 五、用户查询

**路径：** 侧边栏 → 用户查询
**接口：** `GET /api/admin/user/search?keyword=xxx&page=1&size=20`

支持按用户名、邮箱、昵称搜索用户，关键字不传时返回所有用户（分页）。

**curl 测试：**

```bash
curl -s -H "Authorization: Bearer $ADMIN_TOKEN" \
  --get "http://localhost:8085/admin/user/search" \
  --data-urlencode "keyword=alice" \
  --data-urlencode "page=1" \
  --data-urlencode "size=10"
```

---

## 六、推送配置

**路径：** 侧边栏 → 推送配置

### 6.1 推荐权重配置

**接口：** `PUT /api/admin/config`

| 配置 Key | 默认值 | 说明 |
|----------|--------|------|
| `recommend.hot_weight` | `0.6` | 同城热点权重（0~1） |
| `recommend.interest_weight` | `0.4` | 用户兴趣权重（0~1） |
| `map_tag_mapping` | JSON | 灵感分类→地图搜索关键词 |

权重之和建议为 1。例如 `hot_weight=0.6` + `interest_weight=0.4` = 1。

**curl 测试：**

```bash
curl -s -X PUT -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"id":1,"value":"0.8"}' \
  "http://localhost:8085/admin/config"
```

### 6.2 手动推送

**接口：** `POST /api/admin/config/push`

向指定城市的用户推送灵感消息。当前为模拟推送，仅记录日志。后续对接 MQ 后实现真实推送。

**curl 测试：**

```bash
curl -s -X POST -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"title":"今日推荐","content":"长沙同城热门灵感","city":"长沙"}' \
  "http://localhost:8085/admin/config/push"
```

---

## 七、后端接口一览

| 方法 | 路径 | 说明 | 需要登录 |
|------|------|------|---------|
| POST | `/api/admin/login` | 管理员登录 | 否 |
| GET | `/api/admin/dashboard` | 监控大屏数据 | 是 |
| GET | `/api/admin/inspire/list` | 灵感列表（搜索+分页） | 是 |
| PUT | `/api/admin/inspire/{id}/block` | 下架灵感 | 是 |
| PUT | `/api/admin/inspire/{id}/unblock` | 上架灵感 | 是 |
| GET | `/api/admin/user/search` | 搜索用户 | 是 |
| GET | `/api/admin/user/{id}` | 用户详情 | 是 |
| GET | `/api/admin/config/list` | 配置列表 | 是 |
| PUT | `/api/admin/config` | 更新配置值 | 是 |
| POST | `/api/admin/config/push` | 手动推送 | 是 |

**Swagger：** `http://localhost:8085/swagger-ui/index.html`

---

## 八、常见问题

| 问题 | 原因 | 解决 |
|------|------|------|
| 登录返回 401 | 网关白名单没配 `/api/admin/login` | `mvn spring-boot:run -pl inspire-gateway` |
| 下架报"灵感不存在" | JS 精度丢失 | 检查 `id` 字段是否序列化为字符串 |
| 配置页报 SQL 错误 | `desc` 是保留关键字 | `@TableField("\`desc\`")` 加反引号 |
| `admin_user` 表不存在 | `schema.sql` 未执行 | 手动建表或重启 admin 服务 |
| 管理员密码忘记 | 数据库硬改 | `UPDATE admin_user SET password='新BCrypt' WHERE username='admin'` |
