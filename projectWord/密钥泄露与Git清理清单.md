# 密钥泄露与 Git 清理清单

> 更新时间：2026-09-24  
> 目标：把本地开发便利配置与公网运行配置隔离，并列出需要轮换和清理的历史泄露项。

---

## 一、结论

已经完成代码层的配置隔离：

- Docker Compose 不再保存真实密码和密钥。
- 本地密码和长登录白名单改由未跟踪的 `.env` 注入。
- 公网运行使用单独的 `.env.public`。
- 前端账号白名单已删除，长登录策略由后端返回。
- 业务端口已经绑定到 `127.0.0.1`，不再监听 `0.0.0.0`。
- `out/` 和 `.idea/` 已从 Git 索引移除，并加入忽略。

但仍然必须人工完成两件事：

```text
1. 轮换已经进入 Git 历史或被公开过的真实密钥。
2. 使用 git-filter-repo / BFG 清理 Git 历史。
```

只删除当前文件中的值，不能清除 Git 历史。

---

## 二、已经确认涉及的泄露项

### 2.1 SMTP 邮箱授权码

涉及值：

```text
INSPIRE_MAIL_PASSWORD
```

历史位置：

```text
docker-compose.yml
.env.example
backend/inspire-auth/src/main/resources/application.yml
out/production/...
```

处理：

- 立即在邮箱服务商后台撤销旧授权码。
- 重新生成 SMTP 授权码。
- 只写入本机未跟踪的 `.env` / `.env.public`。

### 2.2 JWT 默认密钥

涉及值：

```text
INSPIRE_JWT_SECRET
INSPIRE_ADMIN_JWT_SECRET
JWT_SECRET
```

历史位置：

```text
docker-compose.yml
.env.example
backend/inspire-auth/src/main/resources/application.yml
backend/inspire-admin/src/main/resources/application.yml
backend/inspire-gateway/src/main/resources/application-dev.yml
backend/inspire-gateway/src/main/resources/application-prod.yml
out/production/...
```

处理：

- 重新生成至少 256 位随机密钥。
- 更新本地和公网环境文件。
- 所有旧 AccessToken / RefreshToken 会失效，需要重新登录。
- 轮换后清空 Redis 中 `refresh:*` 和 `user_refresh:*`，避免旧 RefreshToken 继续换取新 AccessToken。

### 2.3 MySQL 默认密码

涉及值：

```text
123456
Inspire@2026
```

历史位置：

```text
docker-compose.yml
.env.example
backend/*/src/main/resources/application-dev.yml
projectWord/middlewareDoc.md
out/production/...
```

处理：

- 如果这些密码曾经用于真实机器，必须轮换。
- 如果只是本地 Docker，可保留“本地新密码”，但不要再提交。
- 创建应用专用账号，不要长期使用 root。
- 修改 MySQL root 密码后，需要执行 `ALTER USER`；如果选择重建数据卷，则重新执行 v2 初始化与种子生成。

### 2.4 Redis 默认密码

涉及值：

```text
123456
Redis@Inspire2026
```

历史位置：

```text
docker-compose.yml
backend/inspire-*/src/main/resources/application*.yml
backend/flink-jobs/...
projectWord/middlewareDoc.md
out/production/...
```

处理：

- 重新生成 Redis 密码。
- Flink、网关、认证、AI、搜索结果缓存统一从环境变量读取。

### 2.5 MinIO 默认凭据

涉及值：

```text
MINIO_ROOT_USER
MINIO_ROOT_PASSWORD
```

历史位置：

```text
docker-compose.yml
docker/minio/docker-compose.yml
docker/minio/init-public-policy.sh
reset-data.sh
.env.example
backend/inspire-core/src/main/resources/application.yml
projectWord/...
out/production/...
```

处理：

- 修改 MinIO root 凭据。
- 脚本统一使用容器内环境变量。
- 不要继续使用默认 `minioadmin/minioadmin123`。
- 已经创建的 MinIO 数据卷需要同步轮换 root 凭据，或在确认可清空图片后重建数据卷。

### 2.6 Unsplash Access Key

历史位置：

```text
backend/inspire-core/src/main/resources/application-dev.yml
~/.bash_profile
```

处理：

- 撤销并重新生成 Unsplash Access Key。
- 只从：
  ```text
  INSPIRE_UNSPLASH_ACCESS_KEY
  ```
  注入。
- `~/.bash_profile` 中旧的 `INSPIRE_UNSPLASH_ACCESS_KEY` 导出已移除。
- `start.sh` / `reset-data.sh` 会先 `unset` 继承的旧 shell 变量，确保 Docker 只读取项目 `.env`。

### 2.7 前端长登录账号白名单

历史位置：

```text
frontend/src/utils/tokenStorage.js
```

内容性质：

```text
user001
user002
user003
admin
```

这不是密码泄露，但暴露了演示账号范围。

当前已修复：

- 前端不再保存账号白名单。
- 后端根据 `INSPIRE_SESSION_LONG_LIVED_USERS` 决定长登录。
- 登录响应返回 `longLived` 标志。

### 2.8 IDE 和编译产物

历史位置：

```text
.idea/
out/
```

风险：

- IDE 数据源配置可能包含数据库连接和凭据。
- `out/` 是旧编译副本，可能包含旧密码。

当前处理：

- 已执行：
  ```bash
  git rm -r --cached .idea
  git rm -r --cached out
  ```
- 本地文件保留。
- `.gitignore` 已忽略 `.idea/` 和 `out/`。

---

## 三、当前配置结构

### 本地开发

使用：

```text
.env
```

该文件已经加入 `.gitignore`。

本地运行：

```bash
docker compose --env-file .env up -d
```

本地允许：

```text
INSPIRE_DEMO_SEED=true
INSPIRE_SESSION_LONG_LIVED_USERS=user001,user002,user003,admin
```

### 公网隧道

复制模板：

```bash
cp .env.public.example .env.public
```

填写真实值后运行：

```bash
docker compose --env-file .env.public up -d
```

公网必须：

```text
SPRING_PROFILES_ACTIVE=prod
INSPIRE_DEMO_SEED=false
INSPIRE_SESSION_LONG_LIVED_USERS=
```

Cloudflare Tunnel 只应访问网关：

```text
gateway:8080
```

不应直接暴露：

```text
auth:8081
core:8083
admin:8085
mysql:3306
redis:6379
minio:9000
```

---

## 四、需要立即轮换的清单

| 项目 | 是否必须轮换 | 原因 |
| --- | --- | --- |
| SMTP 授权码 | 必须 | 真实授权码进入 Git |
| JWT 密钥 | 必须 | 可用于伪造身份令牌 |
| MySQL 密码 | 如果曾用于非本地环境，必须 | 可能被用于数据库连接 |
| Redis 密码 | 如果曾用于非本地环境，必须 | 可影响会话和缓存 |
| MinIO 凭据 | 必须 | 可上传/覆盖图片 |
| Unsplash Key | 建议必须 | 可能被滥用配额 |
| 前端账号白名单 | 无需轮换 | 只是信息暴露 |
| 测试用 `123456` | 视环境而定 | 仅测试则低风险，生产使用则必须 |

---

## 五、Git 历史清理

仓库清理前先备份：

```bash
git clone --mirror <repo-url> repo-backup.git
```

推荐安装 `git-filter-repo` 后处理。

### 1. 删除敏感路径

```bash
git filter-repo \
  --path out/ \
  --path .idea/ \
  --invert-paths
```

### 2. 替换已泄露字符串

创建不提交的 `replacements.txt`：

```text
旧SMTP授权码==>REDACTED
旧JWT密钥==>REDACTED
旧MinIO密码==>REDACTED
旧MySQL密码==>REDACTED
旧Redis密码==>REDACTED
旧UnsplashKey==>REDACTED
```

执行：

```bash
git filter-repo --replace-text replacements.txt
```

### 3. 清理后推送

```bash
git push --force --all
git push --force --tags
```

所有协作者必须重新克隆仓库，旧 clone 不能继续作为安全版本使用。

---

## 六、验收标准

### 配置安全

- [ ] 已跟踪文件中没有真实密码和密钥。
- [ ] `.env`、`.env.public` 均未进入 Git。
- [ ] `docker compose config` 只显示变量替换结果，不在源码中显示真实值。
- [ ] 业务端口只绑定 `127.0.0.1`。
- [ ] 公网只通过网关访问。

### 长登录

- [ ] 前端没有账号白名单。
- [ ] 后端只在本地 dev 使用 `INSPIRE_SESSION_LONG_LIVED_USERS`。
- [ ] `.env.public` 中该变量为空。
- [ ] 前端根据后端 `longLived` 标志处理会话。

### Git

- [ ] `.idea/` 不再跟踪。
- [ ] `out/` 不再跟踪。
- [ ] 已轮换所有需要轮换的密钥。
- [ ] 已完成历史重写和强制推送。
- [ ] 所有协作者已重新克隆。

---

## 七、后续建议

1. 使用 GitHub Secret Scanning / gitleaks 做提交前扫描。
2. 在 CI 中增加 secret scan。
3. 本地 IDE 的数据库连接配置不要再提交。
4. 将 MinIO、MySQL、Redis 分为本地和公网两套凭据。
5. 公网 profile 禁止演示种子和长登录白名单。
6. 后续可接入 Vault、云 Secret Manager 或 GitHub Actions Secrets。
