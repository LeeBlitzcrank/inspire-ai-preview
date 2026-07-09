# Inspire AI Preview

灵感记录与分享平台 — 支持 AI 创作辅助、灵感推荐、社交互动。

## 技术栈

| 层 | 技术 |
|---|---|
| 前端 | Vue 3 + Vite + Element Plus + Vue Router |
| 后端 | Spring Boot 3.2 + MyBatis-Plus + JDK 21 |
| 数据库 | MySQL 8.0 |
| 搜索 | Elasticsearch 7.17 |
| 消息队列 | RocketMQ 5.2 |
| AI | DeepSeek API + Unsplash API |
| 部署 | GitHub Pages（前端）+ Docker Compose / 本地运行（后端） |

## 项目结构

```
├── frontend/               # Vue 3 前端
├── backend/                # Spring Boot 后端（多模块）
│   ├── inspire-gateway/    # API 网关（端口 8080）
│   ├── inspire-auth/       # 用户认证（端口 8081）
│   ├── inspire-ai/         # AI 服务（端口 8082）
│   ├── inspire-core/       # 灵感核心（端口 8083）
│   ├── inspire-admin/      # 后台管理（端口 8085）
│   ├── inspire-search/     # 搜索服务（端口 8086）
│   ├── inspire-common/     # 公共模块
│   └── inspire-mq/         # 消息队列
├── docker/
│   ├── init/
│   │   └── init.sql        # Docker MySQL 初始化脚本（35 张表）
│   └── Dockerfile          # 后端服务 Dockerfile
├── docker-compose.yml       # 一键启动全部服务
├── .env.example             # 环境变量文档
└── projectWord/             # 项目文档目录
```

## 前置要求

| 工具 | 版本 | 用途 |
|------|------|------|
| JDK | 21 | 编译运行后端 |
| Maven | 3.9+ | 构建后端 |
| Node.js | 20+ | 运行前端 |
| Docker Desktop | 最新 | 一键启动（可选） |

**不需要** 单独安装 MySQL / RocketMQ / Elasticsearch —— Docker 会处理。

## 环境变量

所有敏感配置通过环境变量注入，**不要直接修改配置文件**。

| 变量 | 用途 | 默认值 |
|------|------|--------|
| `INSPIRE_DEEPSEEK_API_KEY` | AI 创作（必填） | — |
| `INSPIRE_UNSPLASH_ACCESS_KEY` | AI 配图（可选） | — |
| `INSPIRE_DB_PASSWORD` | MySQL 密码 | `123456` |
| `INSPIRE_REDIS_PASSWORD` | Redis 密码 | `123456` |
| `INSPIRE_JWT_SECRET` | JWT 密钥 | 默认 dev 密钥 |
| `VITE_API_BASE` | 生产环境 API 地址 | — |

完整列表见 `.env.example`。

## 快速开始

### 方式一：Docker Compose（推荐）

```bash
# 1. 构建后端 JAR
cd backend && mvn package -DskipTests && cd ..

# 2. 首次启动（创建数据库和表）
docker compose down -v                     # 删旧数据卷
docker compose up -d mysql                 # 起 MySQL
echo "等待 MySQL 初始化 20 秒..."
sleep 20
docker compose up -d --force-recreate      # 起全部

# 3. 查看启动状态
docker compose logs --tail=5 inspire-core | grep "Started"
docker compose ps
```

MySQL 初次启动时会执行 `docker/init/init.sql` 创建全部 35 张表。

### 方式二：本地开发

**数据库：**

```bash
# 用 Docker 只起基础设施
docker compose up -d mysql elasticsearch rocketmq-namesrv rocketmq-broker
```

**后端（逐个启动）：**

```bash
cd backend

# 先编译
mvn compile -pl inspire-core -am

# 逐个启动（开 6 个终端）
export INSPIRE_DEEPSEEK_API_KEY=你的key
export INSPIRE_UNSPLASH_ACCESS_KEY=你的key

mvn spring-boot:run -pl inspire-auth          # 8081
mvn spring-boot:run -pl inspire-ai            # 8082
mvn spring-boot:run -pl inspire-core          # 8083
mvn spring-boot:run -pl inspire-admin         # 8085
mvn spring-boot:run -pl inspire-search        # 8086
mvn spring-boot:run -pl inspire-gateway       # 8080
```

**前端：**

```bash
cd frontend
npm install
npm run dev      # http://localhost:5173
```

## 数据库说明

### 表结构

共 **35 张表**，按功能分组：

| 模块 | 表 | 数量 |
|------|-----|------|
| 灵感核心 | `inspire_main`, `inspire_content`, `inspire_version` | 3 |
| 收藏（分表） | `collect_0` ~ `collect_9`, `collect_folder` | 11 |
| 点赞（分表） | `inspire_like_0` ~ `inspire_like_9` | 10 |
| 用户 | `user`, `password_reset` | 2 |
| 评论 | `inspire_comment` | 1 |
| 关注 | `user_follow` | 1 |
| 消息 | `message_conversation`, `message` | 2 |
| 通知 | `user_notification` | 1 |
| AI 调用 | `ai_call_log` | 1 |
| 管理 | `admin_user`, `admin_config` | 2 |
| 其他 | 同步/通知/配置相关 | 1 |
| **合计** | | **35** |

### 修改表结构

**Docker 环境：** 修改 `docker/init/init.sql`，然后 `docker compose down -v && docker compose up -d mysql` 重新初始化。

**本地环境：** 修改对应模块的 `src/main/resources/schema.sql`，重启后 Spring Boot 会自动执行。

## 部署

### 前端（GitHub Pages）

```bash
cd frontend

# 设置生产环境 API 地址
echo "VITE_API_BASE=https://你的穿透地址" > .env.production

# 构建并部署
npm run build

# 推送后 GitHub Actions 自动部署
git push
```

仓库 **Settings → Pages** 选 **GitHub Actions** 作为 Source。自定义域名在 Pages 设置里配置。

### 后端（内网穿透）

开发环境通过 ngrok 暴露：

```bash
ngrok http 8080
# 得到 https://xxxx.ngrok.io
```

填入前端 `.env.production` 的 `VITE_API_BASE`。

### GitHub Actions 变量

在仓库 **Settings → Secrets and variables → Actions → Variables** 添加：

| 变量名 | 值 |
|--------|-----|
| `VITE_API_BASE` | `https://你的ngrok地址` |

## 常见问题

### `Connection refused`

MySQL 还没就绪。后端服务配置了 `restart: unless-stopped`，会自动重试。等 20-30 秒即可。

### 数据库表缺失

Docker 环境执行 `docker compose down -v` 清空数据卷，重新初始化。

### Unsplash API 返回 410

检查 `.env.production` 或环境变量中 `INSPIRE_UNSPLASH_ACCESS_KEY` 是否正确。在 [unsplash.com/developers](https://unsplash.com/developers) 获取。

### GitHub Actions 构建失败

1. 检查仓库 **Actions → Variables** 是否有 `VITE_API_BASE`
2. 检查 GitHub Pages 设置是否为 **GitHub Actions** 模式
3. 检查 `.env.production` 是否被 git 跟踪（不应跟踪）

---

*文档更新时间：2026-07-07*
