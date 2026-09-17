# Inspire AI — 鉴权体系完整说明

> **版本**: V1.0 | **更新**: 2026-07-14  
> **架构**: Gateway 统一鉴权 + JWT 双Token + Redis 会话管控 + 下游零JWT依赖  
> **设计文档**: [25、JWT双Token认证体系.md](./project%20Progress/25%E3%80%81JWT%E5%8F%8CToken%E8%AE%A4%E8%AF%81%E4%BD%93%E7%B3%BB.md)

---

## 目录

- [1. 整体架构](#1-整体架构)
- [2. 双Token 核心概念](#2-双token-核心概念)
- [3. 网关鉴权过滤器（JwtAuthGlobalFilter）](#3-网关鉴权过滤器jwtauthglobalfilter)
- [4. Auth 认证服务](#4-auth-认证服务)
- [5. Redis 会话缓存模型](#5-redis-会话缓存模型)
- [6. 下游服务用户上下文（UserContext）](#6-下游服务用户上下文usercontext)
- [7. 前端 Token 管理](#7-前端-token-管理)
- [8. 五大核心流程详解](#8-五大核心流程详解)
- [9. 权限控制（RBAC）](#9-权限控制rbac)
- [10. 错误码定义](#10-错误码定义)
- [11. 各模块鉴权依赖关系](#11-各模块鉴权依赖关系)

---

## 1. 整体架构

```
┌──────────────────────────────────────────────────────────────────┐
│                        前端（Vue 3 + Axios）                      │
│  ┌─────────────────────┐  ┌──────────────────────────────────┐  │
│  │ tokenStorage.js     │  │ request.js（Axios 拦截器）        │  │
│  │  accessToken: 内存   │  │  • 请求拦截器：自动注入 Token     │  │
│  │  refreshToken: LS   │  │  • 响应拦截器：401 自动刷新      │  │
│  │  记住账号: LS 持久化  │  │  • 15分钟无操作自动登出          │  │
│  └─────────────────────┘  └──────────────────────────────────┘  │
└──────────────────────────┬───────────────────────────────────────┘
                           │ POST /api/auth/login
                           │ GET  /api/inspire/public/list
                           │ Authorization: Bearer {accessToken}
                           ▼
┌──────────────────────────────────────────────────────────────────┐
│                网关层（inspire-gateway :8080）                     │
│                                                                  │
│  JwtAuthGlobalFilter（GlobalFilter, Ordered）                    │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │ ① 白名单匹配 → 放行（仍尝试解析 Token 透传用户信息）       │   │
│  │ ② Authorization: Bearer 存在性校验 → 401001              │   │
│  │ ③ Redis 黑名单 black_token:{accessToken} → 401002        │   │
│  │ ④ JWT 签名校验（HS256 全局密钥）→ 401003                  │   │
│  │ ⑤ JWT 过期校验 → 401004                                  │   │
│  │ ⑥ 透传 X-User-Id / X-Inspire-UserId / X-User-Role        │   │
│  │ ⑦ 转发请求到下游微服务                                    │   │
│  └──────────────────────────────────────────────────────────┘   │
│                                                                  │
│  其它：CorsConfig（跨域）、RateLimiterConfig（IP/用户限流）       │
│        GatewayExceptionHandler（全局异常 JSON）                  │
└────────────────┬───────┬───────┬───────┬───────┬────────────────┘
                 │       │       │       │       │
          ┌──────▼┐ ┌──▼───┐ ┌─▼────┐ ┌▼────┐ ┌▼──────┐
          │ Auth  │ │ Core │ │ AI   │ │Admin│ │Search │
          │ :8081 │ │:8083 │ │:8082 │ │:8085│ │:8086  │
          └───┬───┘ └──┬──┘ └──┬───┘ └──┬──┘ └──┬─────┘
              │        │       │        │       │
              │   ┌────┴───────┴────────┴───────┘
              │   │   所有下游服务共用 inspire-common
              │   │   UserContext、PermissionUtil
              │   ▼
         ┌────┴──────┐
         │  Redis    │
         │  • refresh:{rt} → userId    7天
         │  • user_refresh:{uid} → rt  7天
         │  • black_token:{at} → 1     JWT剩余秒
         └───────────┘
```

---

## 2. 双Token 核心概念

### 2.1 Token 定义

| 令牌 | 格式 | 有效期 | 存储位置（前端） | 用途 |
|------|------|--------|-----------------|------|
| **AccessToken** | JWT (HS256) | **15 分钟** | 内存变量（默认）/ localStorage（记住账号） | 业务接口鉴权 |
| **RefreshToken** | 32位 UUID 随机字符串 | **7 天** | localStorage | 刷新 AccessToken、登出签名 |

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

| Claim | 类型 | 说明 |
|-------|------|------|
| `sub` | String | 用户ID（雪花ID） |
| `userName` | String | 用户名 |
| `role` | String | 角色标识：`admin` / `core` / `user` |
| `iat` | Number | 签发时间戳 |
| `exp` | Number | 过期时间戳（15分钟后） |

**安全约束**：JWT 载荷仅存放非敏感标识，禁止手机号、邮箱、密码。

### 2.3 密钥管理

- 加密算法：HS256
- 密钥来源：环境变量 `INSPIRE_JWT_SECRET`（Docker 注入），开发/生产隔离
- 密钥要求：Base64 编码，≥256 位（32 字节）
- 默认开发密钥在 `.env.example` 中

---

## 3. 网关鉴权过滤器（JwtAuthGlobalFilter）

### 3.1 类信息

| 属性 | 值 |
|------|-----|
| 类名 | `JwtAuthGlobalFilter` |
| 包路径 | `inspire-gateway/.../filter/` |
| 实现接口 | `GlobalFilter`, `Ordered` |
| 优先级 | `-200`（最高优先级，在限流之前） |
| 行数 | ~210 行 |

### 3.2 七步鉴权流程（严格顺序）

```
请求到达网关
    │
    ▼
┌────────────────────────────────────────────────────────────────┐
│ 步骤①：白名单匹配                                               │
│                                                                │
│ 匹配 white-list 列表中的路径：                                   │
│ /api/auth/login, /api/auth/register, /api/auth/refresh,        │
│ /api/auth/logout, /uploads/**, /favicon.ico,                   │
│ /actuator/health/**, /actuator/info,                           │
│ /swagger-ui/**, /v3/api-docs/**,                               │
│ /api/inspire/public/**, /api/search/public/**,                 │
│ /api/admin/public/**                                           │
│                                                                │
│ 匹配成功 → 检查 Authorization 头是否存在：                       │
│  ├─ 存在 → 解析 JWT，提取 userId/role，透传 X-User-Id、         │
│ │          X-Inspire-UserId、X-User-Role（方便详情页识别用户）   │
│  └─ 不存在 → 直接放行（匿名访问）                                │
│                                                                │
│ ★ 关键点：白名单路径即使有 Token 也不做签名/过期校验，            │
│   只做"提取用户信息"（try-catch 保护，解析失败不影响放行）         │
└──────────────────────────┬─────────────────────────────────────┘
                           ▼ 非白名单路径
┌────────────────────────────────────────────────────────────────┐
│ 步骤②：校验 Authorization 请求头                               │
│                                                                │
│ 要求：Authorization: Bearer {accessToken}                      │
│ 缺失 → 返回 401001（未携带登录令牌）                             │
└──────────────────────────┬─────────────────────────────────────┘
                           ▼
┌────────────────────────────────────────────────────────────────┐
│ 步骤③：Redis 黑名单校验                                         │
│                                                                │
│ 查询 Redis：black_token:{accessToken}                          │
│ 存在（已登出/被踢）→ 返回 401002（令牌已失效）                    │
│ 使用 ReactiveStringRedisTemplate 非阻塞查询                     │
└──────────────────────────┬─────────────────────────────────────┘
                           ▼
┌────────────────────────────────────────────────────────────────┐
│ 步骤④ + 步骤⑤：JWT 签名校验 + 过期校验                          │
│                                                                │
│ 调用 jwtUtil.parseToken(token)：                                │
│  ├─ ExpiredJwtException → 401004（令牌过期，前端自动刷新）       │
│  └─ 其他 JwtException → 401003（签名篡改/非法）                  │
│                                                                │
│ ★ 步骤④⑤在 handleJwtValidation() 方法中处理                    │
└──────────────────────────┬─────────────────────────────────────┘
                           ▼
┌────────────────────────────────────────────────────────────────┐
│ 步骤⑥：透传用户 Header                                         │
│                                                                │
│ ServerHttpRequest mutatedRequest = request.mutate()            │
│     .header("X-User-Id", userId)                               │
│     .header("X-Inspire-UserId", userId)                        │
│     .header("X-User-Role", role)                               │
│     .build();                                                  │
│                                                                │
│ 同时移除 Authorization 头（下游无需 JWT）                        │
└──────────────────────────┬─────────────────────────────────────┘
                           ▼
┌────────────────────────────────────────────────────────────────┐
│ 步骤⑦：转发请求到业务微服务                                     │
│                                                                │
│ chain.filter(exchange.mutate().request(mutatedRequest).build())│
└────────────────────────────────────────────────────────────────┘
```

### 3.3 关键代码位置

| 功能 | 方法 | 行号（约） |
|------|------|-----------|
| 白名单检查 + Token 解析 | `filter()` | 83-105 |
| Authorization 校验 | `filter()` | 108-115 |
| 黑名单校验（Reactive Redis） | `filter()` | 118-120 |
| JWT 校验（调用 helper） | `filter()` | 121-128 |
| 透传 Header | `filter()` | 145-150 |
| 白名单路径匹配 | `isWhiteList()` | 186-202 |
| JWT 校验逻辑（helper） | `handleJwtValidation()` | 206-230 |
| 错误响应 | `unauthorizedResponse()` | 232-250 |

---

## 4. Auth 认证服务

### 4.1 接口一览

| 方法 | 路径 | 白名单 | 鉴权方式 | 文档流程 |
|------|------|--------|----------|----------|
| POST | `/auth/login` | 是 | 无 | 流程一：登录 |
| POST | `/auth/register` | 是 | 无 | — |
| POST | `/auth/refresh` | 是 | Refresh-Token Header | 流程三：刷新 |
| POST | `/auth/logout` | 是 | 无（Auth 服务解析 Token） | 流程四：登出 |
| POST | `/auth/admin/kick/{userId}` | 否 | JWT（需 admin 角色） | 流程五：踢人 |
| GET | `/auth/userinfo` | 否 | JWT | — |
| PUT | `/auth/userinfo` | 否 | JWT | — |
| PUT | `/auth/password` | 否 | JWT | — |
| POST | `/auth/forgot-password` | 是 | 无 | — |
| POST | `/auth/reset-password` | 是 | 无 | — |

### 4.2 关键工具类

| 类 | 路径 | 职责 |
|----|------|------|
| `JwtUtil` | `inspire-auth/.../util/` | JWT 生成（HS256，15分钟，含 role claim） |
| `RedisSessionUtil` | `inspire-auth/.../util/` | Redis 会话操作（refresh/user_refresh/black_token） |
| `AuthServiceImpl` | `inspire-auth/.../service/impl/` | 登录/刷新/登出/踢人业务逻辑 |

### 4.3 Auth 服务不经过网关白名单？

注意：`/auth/logout` 和 `/auth/refresh` 在网关白名单中。这意味着：
- 网关不对这些路径做 JWT 校验
- Auth 服务自己处理 Token 解析
- **因此这些接口的请求头中不会有 `X-User-Id`**，Auth 服务通过 `Authorization` 头自己解析

---

## 5. Redis 会话缓存模型

### 5.1 Key 规范

```
# 刷新令牌绑定用户（7天过期）
Key:   refresh:{refreshToken}
Value: userId (String)
TTL:   7 天

# 用户 → 刷新令牌映射（踢人/SSO 查询用，7天过期）
Key:   user_refresh:{userId}
Value: refreshToken (String)
TTL:   7 天

# 失效令牌黑名单（登出/踢人时写入，TTL = JWT 剩余秒）
Key:   black_token:{accessToken}
Value: "1"
TTL:   JWT 剩余过期秒数
```

### 5.2 各操作对应的 Redis 操作

| 操作 | 读取 | 写入 | 删除 |
|------|------|------|------|
| 登录 | — | `refresh:{rt}` → userId（7天）<br>`user_refresh:{uid}` → rt（7天）<br>先删旧 `refresh:{oldRt}`（SSO） | — |
| 刷新 | `refresh:{rt}` → 校验存在 | — | — |
| 登出 | — | `black_token:{at}` → 1（JWT剩余秒） | `refresh:{rt}`<br>`user_refresh:{uid}` |
| 踢人 | `user_refresh:{uid}` → 获取旧rt | — | `refresh:{oldRt}`<br>`user_refresh:{uid}` |
| 鉴权 | `black_token:{at}` → 查黑名单 | — | — |

### 5.3 各模块 Redis 配置

| 模块 | 使用场景 | Redis 类型 |
|------|----------|-----------|
| **inspire-gateway** | 黑名单 `black_token:` 查询 | 响应式 `ReactiveStringRedisTemplate` |
| **inspire-auth** | 会话写入/删除 | 阻塞式 `StringRedisTemplate` |
| **inspire-common** | 通用 Redis 配置（下游可选） | `RedisTemplate<String,Object>` |

---

## 6. 下游服务用户上下文（UserContext）

### 6.1 设计原则

下游业务微服务（core / admin / ai / search）**不引入任何 JWT 依赖**，通过网关透传的请求头获取用户信息。

### 6.2 UserContext 组件

| 组件 | 路径 | 说明 |
|------|------|------|
| `UserContext` | `inspire-common/.../model/` | ThreadLocal 上下文持有器 |
| `UserContextInterceptor` | `inspire-common/.../interceptor/` | HandlerInterceptor，读取请求头注入 UserContext |
| `UserContextWebMvcConfig` | `inspire-common/.../config/` | 自动注册拦截器到所有 Web 应用 |
| `PermissionUtil` | `inspire-common/.../util/` | 角色权限校验工具类 |

### 6.3 UserContext API

```java
// 获取当前登录用户信息
Long userId = UserContext.getUserId();       // 从 X-User-Id 头读取
String role = UserContext.getRole();          // 从 X-User-Role 头读取
boolean isLogin = UserContext.isLogin();      // userId 非 null

// 要求登录（未登录抛 IllegalStateException）
Long userId = UserContext.requireUserId();

// 角色判断
boolean isAdmin = UserContext.hasRole("admin");

// 请求完成后自动清除（Interceptor.afterCompletion）
```

### 6.4 PermissionUtil API

```java
// 要求拥有指定角色之一（否则抛 403001 BusinessException）
PermissionUtil.requireRole("admin");
PermissionUtil.requireRole("admin", "core");

// 要求已登录（否则抛 401001）
PermissionUtil.requireLogin();

// 资源所有者校验（管理员可覆盖）
PermissionUtil.requireOwner(resourceUserId);
```

### 6.5 拦截器行为

- 每个 HTTP 请求到达 Controller 前：
  1. 读取 `X-User-Id` / `X-User-Role` / `X-Inspire-UserId` 请求头
  2. 注入 `UserContext` ThreadLocal
  3. 请求完成后自动 `clear()`
- MQ 消费者 / 定时任务：没有 HTTP 上下文，`UserContext.isLogin()` 返回 false

---

## 7. 前端 Token 管理

### 7.1 存储模型

| 存储项 | 位置 | 持久化 | 说明 |
|--------|------|--------|------|
| `accessToken` | 内存变量 | 否 | 页面刷新后丢失，XSS 无法窃取 |
| `refreshToken` | localStorage | 是 | 7 天内可恢复会话 |
| `记住账号` | localStorage | 是 | 额外持久化 accessToken |

**默认模式（不勾选记住账号）**：
- accessToken 只在内存 → 页面刷新后丢失
- refreshToken 在 localStorage → 页面刷新后用 refreshToken 续期

**记住账号模式（勾选后）**：
- accessToken 额外存一份到 localStorage（`inspire_access_token`）
- 页面刷新后从 localStorage 恢复 accessToken
- 用户关闭浏览器再打开也无需重新登录（7天内）

### 7.2 前端文件

| 文件 | 说明 |
|------|------|
| `utils/tokenStorage.js` | Token 存储 + 活跃时间戳 + 记住账号 |
| `utils/request.js` | Axios 实例 + 401 自动刷新 + 请求排队 |
| `api/auth.js` | 登录/注册/刷新/登出 API |

### 7.3 请求拦截器流程

```
请求发出
  │
  ▼
检查 15 分钟无操作 → 过期则清空 Token 跳登录
  │
  ▼
getAccessToken() → 内存中有？ → 有 → 注入 Authorization: Bearer
  │                                   │
  │ 无                                ▼
  ▼                              发出请求
检查 refreshToken → 有 → 自动调 /auth/refresh
  │                    │
  │ 无                 ▼
  ▼              获取新 accessToken → 注入原请求
  │                    │
  ▼                    ▼
发出请求（无Token）  发出请求（有Token）
```

### 7.4 401 自动刷新流程

```
收到 401 响应
  │
  ▼
检查 code
  │
  ├─ 401004（AccessToken 过期）
  │   ├─ refreshToken 存在 → 调 /auth/refresh
  │   │   ├─ 成功 → 更新内存 Token → 重试原请求
  │   │   └─ 失败 → 清空 Token → 跳登录
  │   └─ refreshToken 不存在 → 清空 Token → 跳登录
  │
  ├─ 401001/002/003/005 → 清空 Token → 跳登录
  │
  └─ 403001 → 弹"无权限"提示
```

### 7.5 多请求排队

当多个请求同时触发 401004 时，仅第一个触发刷新，其余排队等待：

```
请求A（401004）→ 开始刷新 → 锁住
请求B（401004）→ 加入队列等待
请求C（401004）→ 加入队列等待
...
刷新完成 → 新 Token → 通知队列 → A/B/C 全部重试成功
```

---

## 8. 五大核心流程详解

### 8.1 流程一：用户登录

```
POST /auth/login
请求体: { username, password, deviceId? }

1. 查询 MySQL user 表
   ├─ 账号不存在 → 500 "账号或密码错误"
   ├─ status=0（冻结）→ 500 "账号已被冻结"
   └─ BCrypt 密码比对失败 → 500 "账号或密码错误"

2. 生成 AccessToken（JWT, HS256, 15分钟）
   载荷: { sub: userId, userName, role, iat, exp }

3. 生成 RefreshToken（32位 UUID 无横线）

4. SSO 挤旧：查询 user_refresh:{userId}
   ├─ 存在旧 refreshToken → 删除 refresh:{oldRefreshToken}
   └─ 不存在 → 跳过

5. 写入 Redis
   ├─ refresh:{refreshToken} → userId（7天）
   └─ user_refresh:{userId}  → refreshToken（7天）

6. 记录登录日志（login_log 表：IP、设备、UA、结果）

7. 返回双Token + 用户信息
```

### 8.2 流程二：业务接口鉴权访问

```
前端 → Gateway → JwtAuthGlobalFilter（7步流程）→ 下游服务

详细流程见第3章「七步鉴权流程」
```

### 8.3 流程三：无感刷新

```
POST /auth/refresh
Header: Refresh-Token: {refreshToken}

1. RefreshToken 为空 → 401005

2. 查询 Redis refresh:{refreshToken}
   ├─ 不存在 → 401005（强制登录）
   └─ 存在 → 获取 userId

3. 查询用户状态
   ├─ 用户不存在/已删除 → 清除会话 → 401005
   └─ status=0（冻结）→ 清除会话 → 500 "账号已被冻结"

4. 生成全新 AccessToken（15分钟）

5. RefreshToken 复用（不生成新的，文档 4.3.3 第4步）

6. 返回新 AccessToken + 旧 RefreshToken
```

### 8.4 流程四：用户登出

```
POST /auth/logout
Authorization: Bearer {accessToken}
Body: { refreshToken: "..." }

1. 从 Authorization 头提取 AccessToken

2. 解析 JWT
   ├─ Token 有效 → 计算剩余秒数
   │              → 写入 black_token:{accessToken}（TTL=剩余秒数）
   └─ 已过期/非法 → 跳过黑名单

3. 删除 refresh:{refreshToken}

4. 删除 user_refresh:{userId}

5. 返回 "已退出登录"
```

### 8.5 流程五：管理员踢人

```
POST /auth/admin/kick/{userId}
X-User-Role: admin（Gateway 校验）

1. 从 X-User-Role 头校验管理员身份
   └─ 非 admin → 403001

2. 查询 user_refresh:{targetUserId} → 获取旧 refreshToken

3. 删除 refresh:{oldRefreshToken}
4. 删除 user_refresh:{targetUserId}

5. 效果：
   ├─ 用户已下发的 AccessToken 最多 15 分钟后自动过期
   └─ 下次刷新 Token 时 RefreshToken 已被删除 → 401005 → 强制登录
```

---

## 9. 权限控制（RBAC）

### 9.1 角色体系

| 角色 | 标识 | 说明 |
|------|------|------|
| 普通用户 | `user` | 默认注册用户，可发布、收藏、点赞 |
| 内容创作者 | `core` | 预留扩展 |
| 管理员 | `admin` | 后台管理、审核、踢人 |

### 9.2 权限校验方式

**方式一：网关层白名单**
- 白名单路径：无需 Token 即可访问
- 非白名单路径：必须携带有效 JWT

**方式二：Controller 层 @RequestHeader**
- Auth 服务 Controller：读取 `X-User-Id` 获取当前用户
- Core 服务 Controller：读取 `X-Inspire-UserId`（向后兼容）

**方式三：UserContext + PermissionUtil（推荐）**
- 所有下游服务通过 `UserContext.getUserId()` 获取当前用户
- `PermissionUtil.requireRole("admin")` 做角色校验

### 9.3 各接口权限要求

| 接口 | 权限 | 校验方式 |
|------|------|----------|
| `/auth/login` | 匿名 | 白名单 |
| `/auth/register` | 匿名 | 白名单 |
| `/auth/refresh` | 匿名 | 白名单（需 Refresh-Token Header） |
| `/auth/logout` | 匿名 | 白名单（Auth 自行解析） |
| `/auth/admin/kick/{id}` | admin | Gateway + `PermissionUtil.requireRole` |
| `/inspire/public/**` | 匿名 | 白名单（登录用户可获得额外信息） |
| `/inspire/my/**` | 必须登录 | Gateway JWT + `UserContext.requireUserId` |
| `/file/upload` | 必须登录 | Gateway JWT |
| `/admin/**` | admin | Gateway JWT + Admin 内部校验 |

---

## 10. 错误码定义

### 10.1 认证错误（401xxx）

| 错误码 | 描述 | HTTP状态 | 前端行为 | 触发场景 |
|--------|------|----------|----------|----------|
| **401001** | 未携带登录令牌 | 401 | 清空缓存，跳登录 | 请求无 `Authorization` 头 |
| **401002** | 令牌已失效 | 401 | 清空缓存，跳登录 | 黑名单命中（登出/踢人） |
| **401003** | 令牌签名非法、篡改 | 401 | 清空缓存，重新登录 | JWT 签名校验失败 |
| **401004** | AccessToken 已过期 | 401 | **自动调用刷新接口** | JWT `exp` 已过 |
| **401005** | RefreshToken 不存在/过期 | 401 | 强制跳登录 | `refresh:{rt}` 缓存不存在 |

### 10.2 权限错误（403xxx）

| 错误码 | 描述 | 触发场景 |
|--------|------|----------|
| **403001** | 当前角色无接口访问权限 | 非 admin 用户访问管理接口 |
| **403002** | 无权操作其他用户的资源 | `requireOwner` 校验失败 |

### 10.3 响应格式

```json
{
  "code": 401004,
  "msg": "登录令牌已过期，请刷新令牌",
  "data": null
}
```

前端的 `request.js` 根据 `code` 字段（而非 HTTP 状态码）决定处理策略。

---

## 11. 各模块鉴权依赖关系

### 11.1 模块依赖图

```
inspire-auth (JWT签发/Redis会话)
    │
    ├─ inspire-common (Result/ErrorCode/BusinessException)
    │
    ├─ spring-boot-starter-data-redis (Redis会话缓存)
    │
    └─ io.jsonwebtoken (JWT生成解析)

inspire-gateway (统一鉴权)
    │
    ├─ inspire-common (Result/ErrorCode/TokenBlacklistService)
    │
    ├─ spring-cloud-starter-gateway (路由/过滤器)
    │
    ├─ spring-boot-starter-data-redis-reactive (非阻塞Redis黑名单)
    │
    └─ io.jsonwebtoken (JWT解析验证)

inspire-core / inspire-admin / inspire-ai / inspire-search
    │
    ├─ inspire-common
    │   ├─ UserContext (ThreadLocal用户上下文)
    │   ├─ PermissionUtil (角色权限校验)
    │   └─ UserContextInterceptor (自动注入)
    │
    └─ ★ 无 JWT 依赖（仅通过请求头获取用户信息）
```

### 11.2 关键依赖

| 模块 | JWT 依赖 | Redis 依赖 |
|------|----------|-----------|
| inspire-gateway | 校验 | 黑名单查询（非阻塞） |
| inspire-auth | 签发+校验 | 会话缓存（阻塞） |
| inspire-core | **无** | **无**（可选 common） |
| inspire-admin | **无** | **无** |
| inspire-ai | **无** | **无** |
| inspire-search | **无** | **无** |
| inspire-common | **无** | 通用配置（可选） |

---

> **相关文件索引**
>
> | 文件 | 说明 |
> |------|------|
> | `inspire-gateway/.../filter/JwtAuthGlobalFilter.java` | 网关鉴权过滤器 |
> | `inspire-auth/.../util/JwtUtil.java` | JWT 签发/解析工具 |
> | `inspire-auth/.../util/RedisSessionUtil.java` | Redis 会话操作 |
> | `inspire-auth/.../controller/AuthController.java` | 认证接口 |
> | `inspire-auth/.../service/impl/AuthServiceImpl.java` | 认证业务逻辑 |
> | `inspire-common/.../model/UserContext.java` | 用户上下文 |
> | `inspire-common/.../model/ErrorCode.java` | 错误码枚举 |
> | `inspire-common/.../interceptor/UserContextInterceptor.java` | 用户上下文拦截器 |
> | `inspire-common/.../util/PermissionUtil.java` | 权限校验工具 |
> | `inspire-common/.../result/Result.java` | 统一返回（含错误码常量） |
> | `frontend/src/utils/tokenStorage.js` | Token 存储 |
> | `frontend/src/utils/request.js` | Axios 拦截器 |
> | `frontend/src/api/auth.js` | 认证 API |
