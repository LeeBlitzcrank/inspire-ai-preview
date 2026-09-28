# JWT双Token认证体系详细设计文档
## 文档信息
| 项目 | 内容 |
| ---- | ---- |
| 文档名称 | 微服务JWT双Token登录认证方案设计文档 |
| 适用项目 | inspire-ai-preview（Gateway+多SpringBoot微服务+Redis+MySQL） |
| 设计目标 | 解决单JWT无法主动注销、强制踢人、频繁登录、分布式会话同步问题，满足商用系统安全规范 |
| 版本 | V1.0 |
| 编写时间 | 2026-07-13 |
| 依赖中间件 | Spring Cloud Gateway、SpringBoot、Redis、JWT(HS256)、MyBatis-Plus |

# 1 需求概述
## 1.1 业务需求
1. 前后端分离架构，所有接口统一鉴权，未登录禁止访问业务接口；
2. 支持账号密码登录、无感续期、主动登出、后台强制用户下线；
3. 支持单点登录，新设备登录自动挤掉旧设备会话；
4. 分布式微服务架构，多实例部署无需会话同步；
5. 区分短期业务令牌与长期刷新令牌，平衡安全与用户体验；
6. 网关统一鉴权，下游业务服务不重复解析JWT，提升性能。

## 1.2 现存单JWT方案缺陷
1. JWT签发后未过期前无法主动失效，登出/踢人无法立即生效；
2. 令牌有效期过长存在被盗风险，有效期过短用户频繁重新登录；
3. 多微服务每个服务都解析JWT，重复计算浪费资源；
4. 无会话存储，无法实现在线用户管理、单点登录。

## 1.3 方案目标
采用 **AccessToken(JWT短期业务令牌) + RefreshToken(长期刷新令牌)** 双Token架构，结合Redis实现会话管控、令牌黑名单，兼顾无状态优势与服务端可控性，满足商用项目安全、运维、体验需求。

# 2 术语定义
| 术语 | 详细说明 |
| ---- | -------- |
| AccessToken | JWT格式业务令牌，HS256加密，有效期15分钟，用于所有业务接口鉴权 |
| RefreshToken | 32位随机字符串刷新令牌，存入Redis，有效期7天，仅用于刷新AccessToken |
| JWT黑名单 | Redis缓存已失效AccessToken，登出/踢人时写入，TTL与JWT剩余过期时间一致 |
| 网关统一鉴权 | Gateway全局拦截器完成Token校验，解析用户信息透传给下游微服务 |
| 无感刷新 | Access过期时前端携带RefreshToken调用刷新接口，无需用户输入账号密码 |
| 单点登录 | 同一用户新设备登录，删除旧设备RefreshToken，旧设备所有接口直接401 |
| 强制下线 | 后台管理端根据userId清理刷新令牌，用户下次刷新token时强制重登 |

# 3 整体架构设计
## 3.1 全流程文字分步流程图（五大核心流程）
### 流程一：用户账号密码登录流程
1. 前端页面收集账号、密码、设备标识，发起 `POST /auth/login` 请求；
2. 请求转发至 `inspire-auth` 认证服务；
3. auth服务查询MySQL用户表，校验账号是否存在、账号是否冻结、密码加密比对；
4. 校验失败：直接返回登录失败，前端提示账号密码错误；
5. 校验成功：
   1）生成短期AccessToken（JWT，有效期15分钟，载荷仅存放userId、用户名、角色）；
   2）生成32位随机字符串RefreshToken；
6. 操作Redis缓存：
    - 存入 `refresh:{refreshToken}` 键，值为用户ID，过期7天；
    - 存入 `user_refresh:{userId}` 键，值为当前RefreshToken，过期7天；
    - 开启单点登录：若该用户已有登录会话，删除旧RefreshToken缓存，旧设备直接失效；
7. auth服务将 accessToken、refreshToken、过期秒数返回前端；
8. 前端接收令牌，accessToken放内存，refreshToken存入本地存储（生产HttpOnly Cookie）。

### 流程二：正常业务接口鉴权访问流程
1. 前端发起业务接口请求，请求头携带 `Authorization: Bearer AccessToken`；
2. 请求先进入 `inspire-gateway` 网关全局拦截器；
3. 网关第一步判断接口白名单（登录、刷新、静态资源、健康检测等），白名单接口直接放行，跳过鉴权；
4. 业务接口进入鉴权校验链路，第一步校验Redis黑名单：
    - 若当前AccessToken存在黑名单缓存，直接返回401令牌已失效；
5. 无黑名单记录，校验JWT：验证签名合法性、判断令牌是否过期；
    - 签名篡改/令牌过期，返回401，提示令牌过期；
6. JWT校验全部通过，解析出userId、用户角色，写入请求头 `X-User-Id`、`X-User-Role`；
7. 网关转发请求至下游业务微服务（core/ai/admin/search）；
8. 业务服务读取请求头中的用户信息，执行业务逻辑，无需解析JWT；
9. 业务处理完成，将结果原路返回前端展示。

### 流程三：AccessToken过期无感刷新流程
1. 前端收到网关返回401令牌过期响应，自动拦截，不跳转登录页；
2. 前端携带本地存储的RefreshToken，单独调用 `/auth/refresh` 刷新接口；
3. 请求进入auth服务，读取传入的RefreshToken；
4. 查询Redis `refresh:{refreshToken}` 缓存：
    - 缓存不存在/已过期：返回401，强制前端清空令牌、跳转登录；
5. RefreshToken校验合法，读取绑定的userId，生成全新AccessToken；
6. 仅返回新accessToken，RefreshToken复用不更新；
7. 前端替换内存中旧AccessToken，使用新令牌重新发起原业务请求，用户无感知。

### 流程四：用户主动登出流程
1. 用户点击页面退出按钮，前端携带当前AccessToken、RefreshToken调用 `/auth/logout`；
2. auth服务执行两步失效逻辑：
   1）计算AccessToken剩余存活时间，写入Redis黑名单 `black_token:{accessToken}`，过期时间与JWT剩余时间一致；
   2）根据userId删除两条Redis会话缓存：`refresh:{refreshToken}`、`user_refresh:{userId}`；
3. 返回登出成功；
4. 前端清空内存accessToken、本地存储refreshToken，跳转登录页面；
5. 该用户后续所有请求，网关校验黑名单直接拦截返回401。

### 流程五：后台管理员强制用户下线（踢人）流程
1. 管理员在后台管理系统，根据用户ID执行强制下线操作，调用 `/auth/admin/kick/{userId}`；
2. 请求进入auth服务，通过userId查询绑定的RefreshToken；
3. 删除该用户全部RefreshToken相关Redis缓存；
4. 返回踢人成功；
5. 效果分两种：
   1）用户当前AccessToken未过期：短时间内仍可访问接口，一旦调用刷新令牌接口会直接失败；
   2）AccessToken过期后，无法刷新，必须重新登录；
   3）15分钟后旧AccessToken自动过期，彻底失效。

## 3.2 跨组件调用关系说明
1. 鉴权统一收口在Gateway网关，下游所有业务微服务不处理JWT校验逻辑；
2. 所有令牌生成、刷新、失效、踢人逻辑统一由auth服务承担；
3. Redis承担三层缓存：刷新令牌会话、用户绑定刷新令牌、失效AccessToken黑名单；
4. MySQL仅存储用户基础账号、角色、权限静态数据；
5. 前端仅负责存储令牌、捕获过期响应、自动调用刷新接口。

# 4 核心功能详细设计
## 4.1 登录功能
### 4.1.1 入参
```json
POST /auth/login
{
  "username": "账号",
  "password": "密码",
  "deviceId": "设备唯一标识（可选，用于区分多设备）"
}
```
### 4.1.2 处理逻辑
1. 查询MySQL用户，校验账号是否存在、账号是否冻结、密码加密匹配；
2. 账号异常直接返回登录失败，记录登录日志；
3. 生成AccessToken（JWT）
    - 加密算法：HS256
    - 密钥：环境变量注入 `JWT_SECRET`，开发/生产密钥隔离
    - 有效期：900秒（15分钟）
    - JWT载荷Payload（仅存放非敏感数据）
      ```json
      {
        "sub": "userId",
        "userName": "用户名",
        "role": "admin/core/user",
        "iat": 签发时间戳,
        "exp": 过期时间戳
      }
      ```
4. 生成32位UUID随机字符串作为RefreshToken；
5. Redis写入两层缓存（实现单点登录）
   ```
   # key1：刷新令牌映射用户，TTL=7天
   Key: refresh:{refreshToken}  Value: userId
   # key2：用户绑定当前刷新令牌，快速踢人
   Key: user_refresh:{userId}   Value: refreshToken
   ```
6. 若开启单点登录，新登录直接删除旧user_refresh缓存，旧设备会话失效；
7. 返回前端双Token结构
```json
{
  "code": 200,
  "msg": "登录成功",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "refreshToken": "32位随机字符串",
    "expiresIn": 900
  }
}
```
### 4.1.3 前端存储规范
- accessToken：内存存储，不持久化localStorage（防止XSS窃取）；
- refreshToken：生产环境存入HttpOnly Cookie，开发环境临时存localStorage。

## 4.2 网关统一鉴权拦截逻辑
### 4.2.1 白名单放行接口（无需Token）
1. 登录 `/auth/login`
2. 刷新令牌 `/auth/refresh`
3. 登出 `/auth/logout`
4. 静态资源、favicon、健康检查、MQ监控、ES监控
5. 后台开放匿名接口（如首页公开资讯）

### 4.2.2 鉴权校验顺序（严格从上到下）
1. 判断请求头是否携带 `Authorization: Bearer {token}`，无则返回 `401001`；
2. 截取AccessToken，查询Redis黑名单 `black_token:{accessToken}`，存在直接返回 `401002`；
3. 使用全局密钥校验JWT签名，篡改返回 `401003`；
4. 校验JWT过期时间，已过期返回 `401004`；
5. 解析JWT载荷 `userId、role`，写入请求头向下游透传：
    - `X-User-Id: 10001`
    - `X-User-Role: ADMIN`
6. 转发请求至对应业务微服务。

### 4.2.3 统一错误响应格式
```json
{
  "code": 401004,
  "msg": "登录令牌已过期，请刷新令牌",
  "data": null
}
```

## 4.3 Token无感刷新接口
### 4.3.1 接口地址
`POST /auth/refresh`
### 4.3.2 入参
Header携带 `Refresh-Token: xxx`
### 4.3.3 执行逻辑
1. 获取RefreshToken，为空返回401；
2. 查询Redis `refresh:{refreshToken}`，无数据说明会话过期，返回401强制登录；
3. 读取userId，生成全新AccessToken；
4. 不更新RefreshToken（7天有效期内复用，减少Redis写入）；
5. 返回新accessToken给前端，前端替换内存令牌继续业务请求。

## 4.4 用户登出功能
### 4.4.1 接口地址
`POST /auth/logout`
### 4.4.2 处理逻辑
1. 同时接收前端传入的AccessToken、RefreshToken；
2. 解析AccessToken，计算剩余过期时间，写入Redis黑名单：
   ```
   Key: black_token:{accessToken}  Value: 1  TTL=剩余秒数
   ```
3. 删除两条Redis会话缓存：
   ```
   del refresh:{refreshToken}
   del user_refresh:{userId}
   ```
4. 返回登出成功，前端清空本地所有令牌，跳转登录页。

## 4.5 后台强制下线（商用运维必备）
### 4.5.1 接口地址
`POST /auth/admin/kick/{userId}`
### 4.5.2 处理逻辑
1. 后台管理系统根据userId执行踢人操作；
2. 通过userId查询绑定的refreshToken并删除；
3. 该用户所有未过期accessToken无法批量清理（JWT无服务端存储），依靠黑名单机制：
    - 用户下次刷新token会失败，强制重登
    - 已下发未过期accessToken最多15分钟自动失效，安全窗口可控
4. 单点登录：新设备登录时，自动删除旧refreshToken，旧设备接口直接401

## 4.6 Redis缓存Key统一规范
| Key前缀 | Value | 过期时间 | 用途 |
| ---- | ---- | ---- | ---- |
| refresh_{refreshToken} | userId | 7天 | 刷新令牌绑定用户，刷新/登出校验 |
| user_refresh_{userId} | refreshToken | 7天 | 单点登录、踢人快速查找刷新令牌 |
| black_token:{accessToken} | 1 | JWT剩余过期时间 | 失效access黑名单，网关拦截 |

# 5 安全规范（商用上线强制要求）
## 5.1 JWT密钥安全
1. 密钥不写入yml配置文件，通过Docker环境变量 `JWT_SECRET` 注入；
2. 开发、测试、生产三套独立密钥，禁止混用；
3. 密钥长度≥32位，大小写+数字+特殊字符组合。

## 5.2 传输安全
1. 生产环境全站HTTPS加密，禁止HTTP明文传输Token；
2. RefreshToken使用HttpOnly Cookie存储，禁止JS读取，防御XSS窃取；
3. 接口增加SameSite Cookie限制，防止CSRF跨站窃取令牌。

## 5.3 JWT载荷安全
1. 仅存储userId、角色、用户名等非敏感标识；
2. 禁止存放手机号、邮箱、身份证、密码等隐私数据（JWT仅Base64编码，可直接解码查看）。

## 5.4 会话安全控制
1. AccessToken有效期15分钟，缩短非法令牌存活窗口；
2. RefreshToken最长7天，避免长期有效令牌被盗；
3. 登录、刷新接口增加IP限流，防止暴力破解、批量刷令牌；
4. 核心敏感操作（修改密码、删除数据）强制重新校验登录，忽略短期有效JWT。

## 5.5 中间件高可用保障
1. Redis开启主从/集群，持久化RDB+AOF，防止会话数据丢失；
2. Redis配置内存淘汰策略，避免黑名单堆积占用内存；
3. 网关、auth服务多实例部署，鉴权无单点故障。

# 6 微服务代码实现规范
## 6.1 Gateway网关鉴权（核心拦截器）
1. 全局GlobalFilter实现Token拦截；
2. 读取Authorization头、黑名单校验、JWT解析；
3. 校验通过后使用 `ServerWebExchange` 透传用户Header；
4. 统一全局异常处理器，封装401错误响应。

## 6.2 Auth认证服务
1. 封装JWT工具类：生成、校验、解析；
2. RedisTemplate封装会话操作工具类；
3. 登录、刷新、登出、踢人接口统一异常捕获；
4. 用户登录日志入库，记录IP、设备、时间、操作结果。

## 6.3 下游业务微服务（core/ai/admin/search）
1. 不引入JWT依赖，不解析令牌；
2. 通过拦截器读取 `X-User-Id`、`X-User-Role`；
3. 基于角色实现接口权限控制；
4. 所有MQ消费者、定时任务单独指定内部系统标识，跳过登录鉴权。

# 7 错误码统一定义
| 错误码 | 描述 | 前端处理逻辑 |
| ---- | ---- | ---- |
| 401001 | 未携带登录令牌 | 清空本地缓存，跳转登录页 |
| 401002 | 令牌已失效（登出/后台踢人） | 清空缓存，跳转登录 |
| 401003 | 令牌签名非法、篡改 | 清空缓存，重新登录 |
| 401004 | AccessToken已过期 | 自动调用刷新接口续期 |
| 401005 | RefreshToken不存在/过期 | 强制跳转登录页 |
| 403001 | 当前角色无接口访问权限 | 弹窗提示无操作权限 |

# 8 方案优缺点分析
## 8.1 优势
1. 保留JWT无状态特性，分布式多实例无需会话同步，适配Docker微服务集群；
2. 双Token平衡安全与体验，短时效Access降低泄露风险，长时效Refresh减少重复登录；
3. Redis黑名单实现主动注销、强制下线，弥补原生JWT最大缺陷；
4. 网关统一鉴权，下游服务轻量化，减少重复JWT解析开销；
5. 支持单点登录、在线用户管控，满足后台运维需求；
6. 架构成熟，中小企业商用项目通用标准方案，维护成本低。

## 8.2 短板
1. AccessToken未过期前无法立即全局失效，最长等待15分钟自动过期；
2. 整体鉴权依赖Redis，Redis宕机将导致登录、鉴权、刷新全部失效；
3. 前端需要额外封装过期自动刷新逻辑，增加前端开发工作量。

## 8.3 短板配套优化方案
1. Redis部署主从集群，开启持久化，消除单点故障；
2. 修改密码、支付、删除等高危操作强制二次登录校验，忽略短期有效JWT；
3. 缩短AccessToken有效期至15分钟，缩小非法令牌存活窗口；
4. 关键业务增加操作日志，出现令牌泄露可快速追溯操作记录。

# 9 生产环境扩展优化方案
1. **多设备管控**：登录携带deviceId，支持按设备单独清理会话；
2. **登录风控**：记录异常IP、异地登录，触发短信验证码二次验证；
3. **OAuth2.0兼容扩展**：支持第三方微信/支付宝登录，复用双Token鉴权体系；
4. **监控告警**：监控401鉴权错误量，异常突增触发风控告警；
5. **状态清理**：依靠Redis TTL自动清理黑名单、过期会话，无需定时任务清理。

# 10 Docker Compose部署适配说明
1. 网关、auth容器统一注入环境变量 `JWT_SECRET`，密钥保持一致；
2. Redis开启持久化，防止容器重启丢失RefreshToken会话；
3. 所有微服务配置内存限制，防止OOM抢占服务器资源；
4. 开发环境可关闭HTTPS，生产强制配置SSL证书。

# 11 接口清单汇总
| 接口路径 | 请求方式 | 功能 | 是否需要Token |
| ---- | ---- | ---- | ---- |
| /auth/login | POST | 账号密码登录 | 否 |
| /auth/refresh | POST | 刷新AccessToken | 否（携带RefreshToken） |
| /auth/logout | POST | 用户主动登出 | 是 |
| /auth/admin/kick/{userId} | POST | 后台强制用户下线 | 是（管理员权限） |
| /api/** | GET/POST/PUT/DELETE | 所有业务接口 | 是 |