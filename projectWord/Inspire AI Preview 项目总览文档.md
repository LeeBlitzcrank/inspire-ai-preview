# Inspire AI Preview — 项目总览文档

> **版本**: V2.0 | **更新**: 2026-07-13  
> **在线演示**: [ai.20sherry.com](https://ai.20sherry.com)  
> **技术栈**: Spring Boot 3.2 + Vue 3 + MySQL + Elasticsearch + RocketMQ + MinIO

---

## 目录

- [1. 项目定位](#1-项目定位)
- [2. 系统架构](#2-系统架构)
- [3. 技术栈](#3-技术栈)
- [4. 后端模块详解](#4-后端模块详解)
- [5. API 网关路由](#5-api-网关路由)
- [6. 认证体系](#6-认证体系)
- [7. 数据存储](#7-数据存储)
- [8. 前端架构](#8-前端架构)
- [9. 部署架构](#9-部署架构)
- [10. 本地开发指南](#10-本地开发指南)
- [11. 数据库表结构](#11-数据库表结构)

---

## 1. 项目定位

灵感记录与分享平台，支持 AI 创作辅助、灵感推荐、社交互动。用户可发布灵感笔记（支持 AI 辅助生成文案与配图）、浏览推荐流、收藏点赞、关注他人、私信互动。

**核心理念**：将碎片化灵感沉淀为结构化内容，通过 AI 降低创作门槛，通过社交关系放大传播。

---

## 2. 系统架构

```
┌─────────────────────────────────────────────────────────────────────┐
│                        前端 (GitHub Pages)                         │
│                     Vue 3 + Vite + Element Plus                     │
│              ai.20sherry.com → Cloudflare → GitHub Pages            │
└────────────────────────────────┬────────────────────────────────────┘
                                 │ HTTPS /api/*
                                 ▼
┌─────────────────────────────────────────────────────────────────────┐
│                   网关层 (inspire-gateway :8080)                    │
│  ┌──────────────┐  ┌──────────────┐  ┌─────────────────────────┐  │
│  │ JWT 鉴权     │  │ 路由分发     │  │ IP 限流 / CORS / 跨域   │  │
│  │ 白名单→黑名单│  │ auth/core/ai │  │                         │  │
│  │ →签名→过期   │  │ admin/search │  │                         │  │
│  └──────────────┘  └──────┬───────┘  └─────────────────────────┘  │
└────────────────────────────┼───────────────────────────────────────┘
          ┌──────────────────┼──────────┬──────────┬──────────┐
          ▼                  ▼          ▼          ▼          ▼
    ┌──────────┐    ┌──────────┐ ┌────────┐ ┌────────┐ ┌────────┐
    │ Auth     │    │ Core     │ │ AI     │ │ Admin  │ │ Search │
    │ :8081    │    │ :8083    │ │ :8082  │ │ :8085  │ │ :8086  │
    ├──────────┤    ├──────────┤ ├────────┤ ├────────┤ ├────────┤
    │ • 登录   │    │ • 灵感   │ │ • 生成 │ │ • 管理 │ │ • ES   │
    │ • 注册   │    │ • 评论   │ │ • 配图 │ │ • 配置 │ │ • 搜索 │
    │ • JWT    │    │ • 收藏   │ │        │ │ • 审核 │ │        │
    │ • 刷新   │    │ • 关注   │ │        │ │       │ │        │
    │ • 登出   │    │ • 消息   │ │        │ │       │ │        │
    │         │    │ • 通知   │ │        │ │       │ │        │
    │         │    │ • MinIO  │ │        │ │       │ │        │
    └──────────┘    └──────────┘ └────────┘ └────────┘ └────────┘
                          │           │
                          ▼           ▼
                   ┌────────────┐ ┌────────────┐
                   │  MySQL     │ │  Redis     │
                   │  35 张表   │ │  会话缓存  │
                   └────────────┘ └────────────┘
                          │
                          ▼
                   ┌────────────┐ ┌────────────┐ ┌────────────┐
                   │ RocketMQ   │ │ES 7.17     │ │ MinIO     │
                   │ 消息队列   │ │ 搜索引     │ │ 图片存储  │
                   └────────────┘ └────────────┘ └────────────┘
```

---

## 3. 技术栈

| 层 | 技术 | 版本 | 说明 |
|----|------|------|------|
| 前端框架 | Vue 3 | ^3.4 | Composition API |
| 构建工具 | Vite | ^5 | 快速开发服务器 |
| UI 组件 | Element Plus | ^2 | 桌面级组件库 |
| 状态路由 | Vue Router | ^4 | 前端路由 |
| HTTP 客户端 | Axios | — | 401 自动刷新拦截 |
| | | | |
| 后端框架 | Spring Boot | 3.2.5 | 微服务架构 |
| JDK | OpenJDK / JBR | 21 | 长期支持版本 |
| ORM | MyBatis-Plus | 3.5.7 | 代码生成器 |
| 网关 | Spring Cloud Gateway | 2023.0.1 | 统一鉴权 |
| 认证 | JWT (jjwt) | 0.12.5 | HS256 双Token |
| 密码加密 | Spring Security Crypto | — | BCrypt |
| | | | |
| **数据库** | MySQL | 8.0 | 主存储 |
| **搜索引擎** | Elasticsearch | 7.17 | 全文搜索 |
| **消息队列** | RocketMQ | 5.2 | 异步解耦 |
| **对象存储** | MinIO | 2024-07 | 图片存储 |
| **缓存** | Redis | 7 | 会话 + 限流 |
| **实时计算** | Apache Flink | 1.17 | 热度计算 |
| | | | |
| 反向代理 | Nginx | 1.27 | 图片缓存层 |
| CDN | Cloudflare | — | 全球加速 |
| 内网穿透 | Cloudflare Tunnel | — | 暴露本地服务 |
| 部署 | Docker Compose | — | 一键启动 |

---

## 4. 后端模块详解

### 4.1 inspire-gateway (端口 8080)

Spring Cloud Gateway 统一入口，所有请求经由此。

**核心功能：**
- **JWT 鉴权过滤器** (`JwtAuthGlobalFilter`)：严格按 白名单→黑名单→签名→过期→透传 顺序校验
- **跨域配置** (`CorsConfig`)：允许前端跨域访问
- **IP 限流** (`RateLimiterConfig`)：基于 Redis 令牌桶，按用户 ID 或 IP 限流
- **全局异常处理** (`GatewayExceptionHandler`)：统一错误 JSON 返回
- **路由分发**：按路径将请求转发到对应微服务

**网关路由表：**

| 路径 | 目标服务 | 说明 |
|------|----------|------|
| `/api/auth/**` | auth:8081 | 认证 |
| `/api/inspire/**` | core:8083 | 灵感核心 |
| `/api/ai/**` | ai:8082 | AI 服务 |
| `/api/admin/**` | admin:8085 | 后台管理 |
| `/api/search/**` | search:8086 | 搜索 |
| `/api/message/**` | core:8083 | 私信 |
| `/api/file/**` | core:8083 | 文件上传 |
| `/api/notification/**` | core:8083 | 通知 |
| `/uploads/**` | core:8083 | 静态文件 |
| `/swagger-ui/**` | auth:8081 | API 文档 |

### 4.2 inspire-auth (端口 8081)

用户认证服务，基于 JWT 双Token 体系。

**核心接口：**

| 方法 | 路径 | 说明 | 文档流程 |
|------|------|------|----------|
| POST | `/auth/login` | 账号密码登录 | 流程一 |
| POST | `/auth/register` | 注册（自动登录） | — |
| POST | `/auth/refresh` | 无感刷新 Token | 流程三 |
| POST | `/auth/logout` | 用户登出 | 流程四 |
| POST | `/auth/admin/kick/{userId}` | 管理员强制下线 | 流程五 |
| GET | `/auth/userinfo` | 获取个人信息 | — |
| PUT | `/auth/userinfo` | 修改个人信息 | — |
| PUT | `/auth/password` | 修改密码 | — |
| POST | `/auth/forgot-password` | 忘记密码 | — |
| POST | `/auth/reset-password` | 重置密码 | — |

**核心工具类：**
- `JwtUtil` — HS256 JWT 签发/解析，15 分钟有效期
- `RedisSessionUtil` — 三层 Redis 会话操作（refresh/user_refresh/black_token）

### 4.3 inspire-core (端口 8083)

灵感核心服务，包含灵感 CRUD、社交互动、文件存储。

**控制器一览：**

| 控制器 | 路径前缀 | 行数 | 主要功能 |
|--------|----------|------|----------|
| `InspireController` | `/inspire` | 341 | 灵感创建、编辑、删除、排行 |
| `FileController` | `/file` | 178 | 文件上传（旧）、图片URL处理 |
| `ImageController` | `/file` | 48 | **MinIO 图片上传 + 私有签名** |
| `CommentController` | `/inspire/{id}/comment` | 49 | 评论 CRUD |
| `FollowController` | `/user/follow` | 65 | 关注/取消关注 |
| `MessageController` | `/message` | 161 | 私信系统 |
| `NotificationController` | `/notification` | 49 | 消息通知 |
| `FileServeController` | — | 35 | 静态文件服务 |
| `InternalController` | `/internal` | 38 | 内部接口 |

### 4.4 inspire-ai (端口 8082)

AI 创作辅助服务。

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/ai/generate` | AI 生成灵感内容 |
| POST | `/ai/select` | AI 选择配图 |
| POST | `/ai/publish` | AI 一键发布 |

依赖：DeepSeek API（文案生成）+ Unsplash API（配图搜索）

### 4.5 inspire-admin (端口 8085)

后台管理系统 API。

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/admin/login` | 管理员登录 |
| GET | `/admin/dashboard` | 仪表盘数据 |
| GET | `/admin/inspire/list` | 灵感列表管理 |
| PUT | `/admin/inspire/{id}/status` | 灵感审核 |
| GET | `/admin/user/list` | 用户管理 |
| PUT | `/admin/config` | 系统配置 |
| POST | `/admin/kick/{userId}` | 强制下线 |

### 4.6 inspire-search (端口 8086)

基于 Elasticsearch 的全文搜索服务。

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/search/public` | 公开搜索 |
| GET | `/search/suggest` | 搜索建议 |

搜索字段：标题、正文、标签。使用 `search_after` 实现深度分页。

### 4.7 inspire-common（公共模块）

被所有微服务引用，提供：

- `Result<T>` — 统一返回结果（含文档标准错误码常量）
- `BusinessException` — 业务异常
- `GlobalExceptionHandler` — 全局异常处理器
- `UserContext` — ThreadLocal 用户上下文
- `UserContextInterceptor` — 自动从请求头注入用户信息
- `PermissionUtil` — 角色权限校验
- `CommonRedisConfig` — 通用 Redis 配置
- `ErrorCode` — 错误码枚举

### 4.8 inspire-mq（消息队列模块）

基于 RocketMQ 的异步消息处理：

- `MqProducer` — 消息生产者
- `MqConsumer` — 消息消费者
- 主题：`topic_user_register`、`topic_user_behavior`、`topic_inspire_publish`
- Flink 消费 `topic_user_behavior` 计算灵感热度

---

## 5. API 网关路由

网关 `application.yml` 中定义的路由规则：

```yaml
spring.cloud.gateway.routes:
  - id: inspire-auth      Path: /api/auth/**      → auth:8081  (StripPrefix=1)
  - id: inspire-ai        Path: /api/ai/**        → ai:8082
  - id: inspire-core      Path: /api/inspire/**   → core:8083
  - id: inspire-admin     Path: /api/admin/**     → admin:8085
  - id: inspire-search    Path: /api/search/**    → search:8086
  - id: inspire-message   Path: /api/message/**   → core:8083
  - id: inspire-file      Path: /api/file/**      → core:8083
  - id: inspire-notification Path: /api/notification/** → core:8083
  - id: inspire-uploads   Path: /uploads/**       → core:8083
  - id: inspire-swagger   Path: /swagger-ui/**,/v3/api-docs/** → auth:8081
```

所有路由均在网关经过鉴权过滤器，**白名单路径**直接放行：

```
/api/auth/login, /api/auth/register, /api/auth/refresh, /api/auth/logout,
/uploads/**, /favicon.ico,
/actuator/health/**, /actuator/info,
/swagger-ui/**, /v3/api-docs/**,
/api/inspire/public/**, /api/search/public/**, /api/admin/public/**
```

---

## 6. 认证体系

### 6.1 双Token 模型

| 令牌 | 格式 | 有效期 | 存储位置 | 用途 |
|------|------|--------|----------|------|
| **AccessToken** | JWT (HS256) | **15 分钟** | 前端内存 | 业务接口鉴权 |
| **RefreshToken** | 32位 UUID 字 | **7 天** | localStorage | 刷新 Token / 登出 |

### 6.2 网关鉴权流程（严格顺序）

```
① 白名单匹配 → 放行
② Authorization: Bearer 存在性校验 → 401001
③ Redis black_token 黑名单校验 → 401002
④ JWT 签名校验 (HS256) → 401003
⑤ JWT 过期校验 → 401004
⑥ 解析 userId/role → 透传 X-User-Id / X-User-Role
⑦ 转发请求到业务微服务
```

### 6.3 下游服务用户上下文

下游服务通过 `inspire-common` 的 `UserContext` 获取当前用户：

```java
// 无需解析 JWT，UserContextInterceptor 自动从 X-User-Id 头读取
Long userId = UserContext.requireUserId();
String role = UserContext.getRole();
boolean isLogin = UserContext.isLogin();
```

### 6.4 前端 Token 处理

`tokenStorage.js` 实现三层安全存储：

- **accessToken**：内存变量（XSS 无法窃取）
- **refreshToken**：localStorage（记住账号模式）
- **活跃时间戳**：15 分钟无操作自动登出

`request.js` 实现 401 自动刷新 + 请求排队：

```
请求无 Token → 请求拦截器检测到 refreshToken → 自动刷新 → 注入新 Token
401004 响应 → 响应拦截器捕获 → 自动刷新 → 重试原请求
401001/002/003/005 → 清空所有令牌 → 跳转登录页
```

---

## 7. 数据存储

### 7.1 MySQL — 35 张表

| 分类 | 表 | 说明 |
|------|-----|------|
| 用户 | `user` | 用户账户 |
| | `password_reset` | 密码重置令牌 |
| | `login_log` | 登录日志 |
| | `admin_user` | 管理员账户 |
| | `admin_config` | 系统配置 |
| 灵感 | `inspire_main` | 灵感主表 |
| | `inspire_content` | 灵感正文 |
| | `inspire_version` | 版本历史 |
| 社交 | `collect_folder` | 收藏文件夹 |
| | `collect_0~9` | 收藏明细（10 张分表） |
| | `inspire_like_0~9` | 点赞明细（10 张分表） |
| | `user_follow` | 用户关注 |
| | `user_notification` | 消息通知 |
| | `message` | 私信 |
| | `message_conversation` | 私信会话 |
| | `like_action` | 点赞动作日志 |
| 文件 | `sys_upload_image` | 图片上传元数据（MinIO） |
| | `ai_call_log` | AI 调用日志 |
| | `inspire_report` | 举报记录 |

### 7.2 Redis — 三层缓存

| Key 前缀 | TTL | 用途 |
|----------|-----|------|
| `refresh:{refreshToken}` | 7 天 | 刷新令牌绑定用户 |
| `user_refresh:{userId}` | 7 天 | 单点登录 + 踢人查询 |
| `black_token:{accessToken}` | JWT 剩余秒 | 失效令牌黑名单 |

### 7.3 MinIO — 图片存储

部署在 Docker，端口 9000 (API) / 9001 (管理后台)。

**目录结构：**
```
my-bucket/
├── upload/
│   └── {userId}/
│       └── {yyyyMMdd}/
│           └── {uuid}.{jpg|png|webp}
└── upload/private/
    └── {userId}/
        └── {yyyyMMdd}/
            └── {uuid}.{jpg|png|webp}
```

**访问链路：**
```
公开图：img.20sherry.com → Cloudflare CDN → Nginx 缓存 → MinIO
私有图：前端 → Gateway → Core(校验权限) → 15分钟签名URL → 前端加载
```

### 7.4 Elasticsearch — 搜索

版本 7.17，用于灵感全文搜索，通过 RocketMQ 异步同步 MySQL 数据。

**索引结构：** `inspire_index` — 包含 `title`（text）、`content`（text）、`tag`（keyword）、`userName` 等字段。

### 7.5 RocketMQ — 消息队列

版本 5.2，命名服务端口 9876，Broker 端口 10911。

| 主题 | 生产者 | 消费者 | 用途 |
|------|--------|--------|------|
| `topic_user_register` | auth | core | 注册后初始化用户数据 |
| `topic_user_behavior` | auth | core, Flink | 登录/行为记录，热度计算 |
| `topic_inspire_publish` | core | admin, Flink | 发布后审核/索引 |

---

## 8. 前端架构

### 8.1 技术栈

- **框架**：Vue 3 (Composition API) + Vite 5
- **UI 库**：Element Plus
- **路由**：Vue Router 4（懒加载路由）
- **HTTP**：Axios（401 自动刷新拦截器）
- **部署**：GitHub Pages

### 8.2 页面路由

| 路径 | 组件 | 需要登录 | 说明 |
|------|------|----------|------|
| `/` | Index | — | 首页推荐流 |
| `/search` | Search | — | 搜索页 |
| `/detail/:id` | InspireDetail | — | 灵感详情 |
| `/login` | Login | — | 用户登录 |
| `/register` | Register | — | 用户注册 |
| `/forgot-password` | ForgotPassword | — | 忘记密码 |
| `/reset-password` | ResetPassword | — | 重置密码 |
| `/personal` | Personal | ✓ | 个人中心 |
| `/notifications` | Notifications | ✓ | 通知列表 |
| `/collections` | Collections | ✓ | 收藏夹 |
| `/messages` | Messages | ✓ | 私信 |
| `/create` | Create | ✓ | 新建灵感 |
| `/edit/:id` | Create | ✓ | 编辑灵感 |
| `/admin/login` | AdminLogin | — | 管理员登录 |
| `/admin` | AdminLayout | ✓ | 后台管理 |

### 8.3 核心模块

| 文件 | 说明 |
|------|------|
| `utils/request.js` | Axios 实例，401 自动刷新 + 请求排队 |
| `utils/tokenStorage.js` | 双Token 安全存储 + 15 分钟活跃检测 |
| `api/auth.js` | 登录/注册/刷新/登出 API |
| `api/inspire.js` | 灵感/评论/收藏/关注 API |

---

## 9. 部署架构

### 9.1 本地开发

```bash
# 启动中间件（Docker）
docker compose up -d mysql redis rocketmq-namesrv rocketmq-broker elasticsearch minio nginx-image

# 启动后端（IDE 依次启动各模块）
inspire-gateway:8080 → inspire-auth:8081 → inspire-ai:8082 → inspire-core:8083
→ inspire-admin:8085 → inspire-search:8086

# 启动前端
cd frontend && npm run dev  # localhost:5173
```

### 9.2 生产部署 (Docker Compose)

```bash
# 构建全部后端 JAR
cd backend && mvn package -DskipTests && cd ..

# 启动全部服务
docker compose up -d --build
```

### 9.3 图片服务部署

```bash
# 单独启动 MinIO + Nginx 缓存层
docker compose -f docker/minio/docker-compose.yml --env-file docker/minio/.env up -d

# 初始化 MinIO 存储桶
docker exec inspire-minio mc alias set local http://localhost:9000 minioadmin minioadmin123
docker exec inspire-minio mc mb local/inspire-img
docker exec inspire-minio mc version enable local/inspire-img

# Cloudflare Tunnel（暴露本地 Nginx）
cloudflared tunnel run inspire-tunnel
```

### 9.4 域名解析

| 域名 | 指向 | 说明 |
|------|------|------|
| `ai.20sherry.com` | GitHub Pages | 前端静态页面 |
| `api.20sherry.com` | Cloudflare Tunnel → localhost:8080 | 后端 API |
| `img.20sherry.com` | Cloudflare Tunnel → localhost:8088 | 图片 CDN |

---

## 10. 本地开发指南

### 10.1 前置条件

```bash
# 版本要求
java --version       # ≥ 21
mvn --version        # ≥ 3.9
node --version       # ≥ 20
docker --version     # 最新
```

### 10.2 环境变量

```bash
# 复制环境变量模板
cp .env.example .env
# 至少配置以下变量（参考 .env.example）
export INSPIRE_DEEPSEEK_API_KEY=your_key
export INSPIRE_UNSPLASH_ACCESS_KEY=your_key
```

### 10.3 一键启动

```bash
git clone <repo>
cd inspire-ai-preview

# 构建
cd backend && mvn package -DskipTests && cd ..
cd frontend && npm install && cd ..

# 首次启动（需等待 MySQL 初始化）
docker compose down -v
docker compose up -d mysql
sleep 20
docker compose up -d

# 启动前端
cd frontend && npm run dev
```

### 10.4 访问地址

| 服务 | 地址 |
|------|------|
| 前端 | http://localhost:5173 |
| 后端 API | http://localhost:8080 |
| Swagger 文档 | http://localhost:8080/swagger-ui.html |
| MinIO 管理后台 | http://localhost:9001 |
| Elasticsearch | http://localhost:9200 |
| RocketMQ 控制台 | http://localhost:9876 |

---

## 11. 数据库表结构

完整数据库表结构见 `database-schema.md`，共 35 张表。核心表概览：

| 表名 | 行数 | 核心字段 |
|------|------|----------|
| `user` | ~ | id, username, password, email, nickname, avatar, role, status |
| `inspire_main` | ~ | id, title, img, images, tag, user_id, status, heat |
| `inspire_content` | ~ | inspire_id, content (TEXT) |
| `collect_folder` | ~ | user_id, name, icon |
| `collect_0~9` | ~ | user_id, inspire_id, folder_id |
| `inspire_like_0~9` | ~ | user_id, inspire_id |
| `user_follow` | ~ | follower_id, following_id |
| `message` | ~ | from_id, to_id, content |
| `sys_upload_image` | ~ | file_key, cdn_url, file_size, is_private, user_id |
| `admin_user` | ~ | username, password, nickname |
| `login_log` | ~ | user_id, ip, login_type, result |

---

> **相关文档索引**
>
> | 文档 | 路径 |
> |------|------|
> | 产品需求文档 | `projectWord/PRD.md` |
> | 数据库表结构 | `database-schema.md` |
> | JWT 双Token 设计 | `projectWord/project Progress/25、JWT双Token认证体系.md` |
> | JWT 认证接口文档 | `projectWord/project Progress/26、JWT双Token认证接口文档.md` |
> | MinIO 图片存储 | `projectWord/project Progress/26、MinIO本地图片存储模块.md` |
> | 网关文档 | `projectWord/getWayDoc.md` |
> | 中间件文档 | `projectWord/middlewareDoc.md` |
> | 部署文档 | `projectWord/project Tool/docker.md` |
