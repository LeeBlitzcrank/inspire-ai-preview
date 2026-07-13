# Inspire AI — JWT 双Token 认证接口文档

> **版本**: V1.0 | **更新**: 2026-07-13  
> **适用项目**: inspire-ai-preview  
> **基础文档**: [25、JWT双Token认证体系.md](./25、JWT双Token认证体系.md)

---

## 目录

- [1. 整体架构](#1-整体架构)
- [2. 核心概念](#2-核心概念)
- [3. API 接口参考](#3-api-接口参考)
- [4. 全局错误码](#4-全局错误码)
- [5. 前端集成指南](#5-前端集成指南)
- [6. 后端集成指南](#6-后端集成指南)
- [7. 部署配置](#7-部署配置)
- [8. 安全规范](#8-安全规范)

---

## 1. 整体架构

```
┌─────────────────────────────────────────────────────────────┐
│                        前端 (Vue)                           │
│  ┌─────────┐   ┌──────────────┐   ┌───────────────────┐   │
│  │ 登录页面 │──▶│ axios 拦截器  │──▶│ tokenStorage.js   │   │
│  │         │   │ 401 自动刷新  │   │ accessToken: 内存 │   │
│  └─────────┘   │ 403 弹提示    │   │ refreshToken: LS  │   │
│                └──────┬───────┘   └───────────────────┘   │
└───────────────────────┼───────────────────────────────────┘
                        │ POST /api/auth/xxx
                        ▼
┌─────────────────────────────────────────────────────────────┐
│              网关层 (inspire-gateway :8080)                  │
│  ┌─────────────────────────────────────────────────────┐   │
│  │ JwtAuthGlobalFilter                                 │   │
│  │  ① 白名单放行                                       │   │
│  │  ② 校验 Authorization: Bearer                       │   │
│  │  ③ Redis黑名单 black_token:{accessToken}             │   │
│  │  ④ JWT 签名校验 (HS256)                             │   │
│  │  ⑤ JWT 过期校验                                     │   │
│  │  ⑥ 透传 X-User-Id / X-User-Role 到下游              │   │
│  └─────────────────────────────────────────────────────┘   │
└────────┬────────┬────────┬────────┬────────────────────────┘
         │        │        │        │
         ▼        ▼        ▼        ▼
   ┌────────┐ ┌──────┐ ┌──────┐ ┌────────┐
   │  Auth  │ │ Core │ │  AI  │ │ Search │   ...
   │ :8081  │ │:8083 │ │:8082 │ │ :8086  │
   └───┬────┘ └──────┘ └──────┘ └────────┘
       │
       ├─ MySQL: 用户表 + 登录日志
       ├─ Redis: 会话缓存 + 黑名单
       └─ 签发: JWT + RefreshToken
```

---

## 2. 核心概念

### 2.1 双Token 模型

| 令牌 | 格式 | 有效期 | 存储位置 | 用途 |
|------|------|--------|----------|------|
| **AccessToken** | JWT (HS256) | **15 分钟** | 前端内存 / `Authorization` 头 | 业务接口鉴权 |
| **RefreshToken** | 32位随机字符串 (UUID) | **7 天** | 前端 localStorage / Header | 刷新 AccessToken、登出 |

### 2.2 JWT 载荷规范

```json
{
  "sub": "10001",
  "userName": "alice",
  "role": "user",
  "iat": 1720800000,
  "exp": 1720800900
}
```

载荷仅存放非敏感标识，**禁止**放入手机号、邮箱、密码等隐私数据。

### 2.3 Redis Key 规范

| Key | Value | TTL | 用途 |
|-----|-------|-----|------|
| `refresh:{refreshToken}` | `userId` | 7天 | 刷新令牌绑定用户 |
| `user_refresh:{userId}` | `refreshToken` | 7天 | 单点登录、踢人查询 |
| `black_token:{accessToken}` | `1` | JWT剩余秒 | 失效 AccessToken 黑名单 |

### 2.4 网关透传 Header

网关校验通过后，向下游微服务透传以下请求头：

| Header | 值示例 | 说明 |
|--------|--------|------|
| `X-User-Id` | `10001` | 用户ID |
| `X-User-Role` | `user` | 角色: `admin` / `core` / `user` |

下游服务通过 **`UserContext.getUserId()`** 获取当前用户，**无需解析 JWT**。

---

## 3. API 接口参考

### 3.1 用户登录

```
POST /auth/login
```

**请求头：** 无（白名单接口）

**请求体：**

```json
{
  "username": "alice",
  "password": "123456",
  "deviceId": "device_abc123"
}
```

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `username` | string | 是 | 登录账号 |
| `password` | string | 是 | 登录密码 |
| `deviceId` | string | 否 | 设备标识（多设备管控） |

**处理流程（文档流程一）：**

```
登录请求
  ├─ 查询 MySQL 用户表
  │   ├─ 账号不存在         → 401 账号或密码错误
  │   ├─ 账号已冻结(status=0)→ 401 账号已被冻结
  │   └─ 密码 BCrypt 比对   → 401 账号或密码错误
  ├─ 生成 AccessToken (JWT, 15min)
  ├─ 生成 RefreshToken (32位 UUID)
  ├─ SSO 挤旧: 删除该用户旧 RefreshToken 会话
  ├─ 写入 Redis
  │   ├─ refresh:{refreshToken} → userId        (7天)
  │   └─ user_refresh:{userId}  → refreshToken  (7天)
  ├─ 记录登录日志 (login_log 表)
  └─ 返回双Token
```

**成功响应 (200)：**

```json
{
  "code": 200,
  "msg": "登录成功",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxMDAwMSJ9...",
    "refreshToken": "a1b2c3d4e5f6a7b8c9d0e1f2a3b4c5d6a7b8c9d0",
    "expiresIn": 900,
    "userId": 10001,
    "username": "alice",
    "nickname": "快乐小鱼",
    "avatar": "🌸",
    "role": "user"
  }
}
```

**失败响应：**

```json
{ "code": 500,  "msg": "账号或密码错误", "data": null }
```

---

### 3.2 刷新 AccessToken

```
POST /auth/refresh
```

**请求头：**

| Header | 值 | 必填 |
|--------|-----|------|
| `Refresh-Token` | `a1b2c3d4e5f6a7b8c9d0e1f2a3b4c5d6a7b8c9d0` | 是 |

**处理流程（文档流程三）：**

```
刷新请求
  ├─ RefreshToken 为空        → 401005
  ├─ 查询 Redis refresh:{token}
  │   └─ 缓存不存在            → 401005 强制登录
  ├─ 查询用户状态
  │   └─ 账号冻结/不存在       → 清除会话，401005
  ├─ 生成全新 AccessToken
  ├─ RefreshToken 复用（不更新）
  └─ 返回新 AccessToken + 旧 RefreshToken
```

> **⚠️ 关键点**：RefreshToken 7 天内复用，不生成新的 RefreshToken，减少 Redis 写入次数。

**成功响应 (200)：**

```json
{
  "code": 200,
  "msg": "刷新成功",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxMDAwMSJ9...",
    "refreshToken": "a1b2c3d4e5f6a7b8c9d0e1f2a3b4c5d6a7b8c9d0",
    "expiresIn": 900,
    "userId": 10001,
    "username": "alice",
    "nickname": "快乐小鱼",
    "avatar": "🌸",
    "role": "user"
  }
}
```

---

### 3.3 用户登出

```
POST /auth/logout
```

**请求头：**

| Header | 值 | 必填 |
|--------|-----|------|
| `Authorization` | `Bearer eyJhbGciOiJIUzI1NiJ9...` | 是 |
| `Content-Type` | `application/json` | 是 |

**请求体：**

```json
{
  "refreshToken": "a1b2c3d4e5f6a7b8c9d0e1f2a3b4c5d6a7b8c9d0"
}
```

**处理流程（文档流程四）：**

```
登出请求
  ├─ 从 Authorization 头提取 AccessToken
  ├─ 解析 JWT 计算剩余秒数
  │   ├─ 有效Token → 写入 black_token:{accessToken} (TTL=剩余秒)
  │   └─ 过期Token → 跳过黑名单（已过期无需处理）
  ├─ 从请求体提取 RefreshToken
  ├─ 删除 refresh:{refreshToken}
  ├─ 删除 user_refresh:{userId}
  └─ 返回登出成功
```

**成功响应 (200)：**

```json
{ "code": 200, "msg": "已退出登录", "data": null }
```

---

### 3.4 管理员强制下线

```
POST /auth/admin/kick/{userId}
```

**请求头：**

| Header | 值 | 说明 |
|--------|-----|------|
| `Authorization` | `Bearer eyJhbGciOiJIUzI1NiJ9...` | 需 admin 角色 |
| `X-User-Id` | `1` | 操作管理员ID（网关透传） |
| `X-User-Role` | `admin` | 操作管理员角色（网关透传） |

**路径参数：**

| 参数 | 类型 | 说明 |
|------|------|------|
| `{userId}` | Long | 被踢下线的用户ID |

**处理流程（文档流程五）：**

```
踢人请求
  ├─ 校验 X-User-Role = admin
  │   └─ 非 admin → 403001
  ├─ 查询 user_refresh:{targetUserId}
  ├─ 删除 refresh:{refreshToken}
  ├─ 删除 user_refresh:{targetUserId}
  └─ 返回强制下线成功
```

> **效果说明**：踢人后，用户已下发的 AccessToken 最多 15 分钟后自动过期。调用刷新接口会立即失败（RefreshToken 已被删除）。

**成功响应 (200)：**

```json
{ "code": 200, "msg": "强制下线成功", "data": null }
```

---

## 4. 全局错误码

| 错误码 | 描述 | 前端处理逻辑 |
|--------|------|-------------|
| **401001** | 未携带登录令牌 | 清空本地缓存，跳转登录页 |
| **401002** | 令牌已失效（登出/后台踢人） | 清空缓存，跳转登录页 |
| **401003** | 令牌签名非法、篡改 | 清空缓存，重新登录 |
| **401004** | AccessToken 已过期 | **自动调用刷新接口**续期 |
| **401005** | RefreshToken 不存在/过期 | 强制跳转登录页 |
| **403001** | 当前角色无接口访问权限 | 弹窗提示无操作权限 |

> 所有错误响应体格式统一：
> ```json
> { "code": 401004, "msg": "登录令牌已过期，请刷新令牌", "data": null }
> ```

---

## 5. 前端集成指南

### 5.1 快速接入

在项目入口安装已完成的工作：

```bash
src/
├── api/
│   └── auth.js           # 登录/刷新/登出 API
├── utils/
│   ├── request.js        # axios 实例 + 401 自动刷新
│   └── tokenStorage.js   # 双Token 安全存储
```

### 5.2 登录页调用示例

```javascript
// src/pages/Login.vue
import { login } from '@/api/auth.js'
import { saveTokens } from '@/utils/tokenStorage.js'

async function handleLogin() {
  try {
    const res = await login({ username: 'alice', password: '123456' })
    if (res.code === 200) {
      // tokenStorage 自动保存（login 方法内已调用 saveTokens）
      router.push('/')
    }
  } catch (e) {
    ElMessage.error('登录失败')
  }
}
```

### 5.3 401 自动刷新流程

`request.js` 已内置完整的自动刷新逻辑：

```mermaid
sequenceDiagram
  participant F as 前端
  participant G as 网关
  participant A as Auth服务

  F->>G: GET /api/inspire/list (带 AccessToken)
  G->>F: 401004 (Token过期)
  
  Note over F: axios 拦截器捕获 401004
  Note over F: 检查 RefreshToken 是否存在
  
  alt RefreshToken 存在
    F->>A: POST /auth/refresh (Refresh-Token 头)
    A->>F: 新 AccessToken
    
    Note over F: 更新内存 Token
    F->>G: 重试原请求 (新 AccessToken)
    G->>F: 200 OK
  else RefreshToken 不存在
    Note over F: 清空所有令牌
    F->>F: 跳转 /login
  end
```

### 5.4 Token 存储规范

```javascript
// tokenStorage.js — 核心存储规则
//
// accessToken: 内存变量（不写 localStorage）
//   - 优势：关闭页面自动丢失，XSS 无法窃取
//   - 劣势：刷新页面需要重新登录（或从 RefreshToken 恢复）
//
// refreshToken: localStorage
//   - 优势：页面刷新后仍可恢复会话
//   - 通过 HttpOnly Cookie 更安全（生产环境建议）
```

### 5.5 登出示例

```javascript
import { logout } from '@/api/auth.js'
import { clearAllTokens } from '@/utils/tokenStorage.js'

async function handleLogout() {
  await logout()                 // 调用后端登出 + 自动 clearAllTokens
  router.push('/login')
}
```

---

## 6. 后端集成指南

### 6.1 下游业务微服务接入

所有业务微服务（core / admin / ai / search）只需依赖 `inspire-common`：

```xml
<dependency>
    <groupId>com.inspire.platform</groupId>
    <artifactId>inspire-common</artifactId>
</dependency>
```

即可使用：

```java
// 获取当前登录用户（网关透传，零JWT依赖）
@GetMapping("/my/profile")
public Result<?> myProfile() {
    Long userId = UserContext.requireUserId();   // 要求登录
    String role = UserContext.getRole();          // 当前角色
    if (UserContext.isLogin()) { ... }
    return Result.success(profileService.getProfile(userId));
}

// 权限校验
@PostMapping("/admin/batch-delete")
public Result<Void> batchDelete(@RequestBody List<Long> ids) {
    PermissionUtil.requireRole("admin");          // 仅 admin 可执行
    PermissionUtil.requireLogin();                // 要求登录
    PermissionUtil.requireOwner(resourceUserId);  // 资源所有者校验
    return Result.success();
}
```

### 6.2 UserContext 拦截器自动注册

在 `inspire-common` 中已自动注册 `UserContextInterceptor`，每个 Web 请求自动：

1. 读取 `X-User-Id` / `X-User-Role` 请求头
2. 注入 `UserContext` ThreadLocal
3. 请求完成后自动 `clear()`

如需手动跳过某些路径（如内部监控端点），在各模块的 `WebMvcConfigurer` 中排除：

```java
@Configuration
public class MyWebConfig implements WebMvcConfigurer {
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new UserContextInterceptor())
                .addPathPatterns("/**")
                .excludePathPatterns("/actuator/**", "/internal/**");
    }
}
```

### 6.3 MQ 消费者 / 定时任务

MQ 消费者和定时任务没有 HTTP 请求上下文，`UserContext.isLogin()` 返回 `false`。如需模拟登录态：

```java
@Component
public class MyScheduledTask {
    @Scheduled(cron = "0 0 2 * * ?")
    public void cleanUp() {
        UserContext.setUserId(0L);        // 系统内部标识
        UserContext.setRole("system");
        try {
            // 执行业务逻辑
        } finally {
            UserContext.clear();          // 务必清理
        }
    }
}
```

---

## 7. 部署配置

### 7.1 环境变量

| 变量名 | 说明 | 开发环境默认值 |
|--------|------|---------------|
| `INSPIRE_JWT_SECRET` | JWT 密钥（Base64, ≥256位） | `dGhpc0lzQURlZmF1bHRT...` |
| `JWT_SECRET` | 同上（文档命名，备选） | 同上 |
| `INSPIRE_JWT_ACCESS_EXPIRATION` | AccessToken 有效期(ms) | `900000` (15分钟) |
| `INSPIRE_REDIS_HOST` | Redis 主机 | `localhost` |
| `INSPIRE_REDIS_PASSWORD` | Redis 密码 | `123456` |

### 7.2 Docker Compose

```yaml
# docker-compose.yml（部分摘录）
x-common-env: &common-env
  JWT_SECRET: ${JWT_SECRET:-dGhpc0lzQURlZmF1bHRT...}
  INSPIRE_JWT_SECRET: ${INSPIRE_JWT_SECRET:-${JWT_SECRET}}
  INSPIRE_JWT_ACCESS_EXPIRATION: 900000

services:
  inspire-gateway:
    environment: *common-env
    ports: ["8080:8080"]

  inspire-auth:
    environment: *common-env
    ports: ["8081:8081"]
```

### 7.3 密钥生成（生产环境）

```bash
# 生成 256 位（32字节）的 Base64 密钥
openssl rand -base64 32
# 输出示例: xK7gP3fR9mZ2qW5yB8nL4tV1cX6sA0jD3eF5hG7iJ8kL

# 设置环境变量（生产环境使用，禁止写死到配置文件）
export JWT_SECRET="xK7gP3fR9mZ2qW5yB8nL4tV1cX6sA0jD3eF5hG7iJ8kL"
```

---

## 8. 安全规范

| 要求 | 措施 |
|------|------|
| **密钥安全** | 密钥通过 Docker 环境变量注入，不写入配置文件；开发/测试/生产三套独立密钥 |
| **传输安全** | 生产环境全站 HTTPS；AccessToken 仅存内存；RefreshToken 生产用 HttpOnly Cookie |
| **载荷安全** | JWT 仅存 userId/role/username，不存手机号、邮箱、密码等隐私 |
| **会话安全** | AccessToken 15 分钟短有效期；RefreshToken 最长 7 天；登录接口 IP 限流 |
| **中间件高可用** | Redis 主从/集群，RDB+AOF 持久化；网关和 auth 多实例部署 |

---

> **相关文件索引**
>
> | 文件 | 说明 |
> |------|------|
> | 设计文档 | `projectWord/25、JWT双Token认证体系.md` |
> | 网关过滤器 | `inspire-gateway/.../filter/JwtAuthGlobalFilter.java` |
> | Auth 服务 | `inspire-auth/.../controller/AuthController.java` |
> | Auth 服务实现 | `inspire-auth/.../service/impl/AuthServiceImpl.java` |
> | JWT 工具类 | `inspire-auth/.../util/JwtUtil.java` |
> | Redis 会话工具 | `inspire-auth/.../util/RedisSessionUtil.java` |
> | 用户上下文 | `inspire-common/.../model/UserContext.java` |
> | 权限工具 | `inspire-common/.../util/PermissionUtil.java` |
> | 全局错误码 | `inspire-common/.../result/Result.java` |
> | 前端请求库 | `frontend/src/utils/request.js` |
> | 前端Token存储 | `frontend/src/utils/tokenStorage.js` |
> | 前端认证API | `frontend/src/api/auth.js` |
> | Postman 测试集 | `docs/postman/Inspire-Auth_DualToken.postman_collection.json` |
