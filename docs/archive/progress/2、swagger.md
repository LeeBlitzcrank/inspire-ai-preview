目前已有的用户端接口：

| 功能 | 状态 |
|------|------|
| 注册（BCrypt加密 + 用户名唯一校验 + JWT下发） | ✅ 已实现 |
| 登录（密码校验 + JWT下发） | ✅ 已实现 |
| 获取当前用户信息 | ✅ 已实现 |
| JWT 7天过期 | ✅ 已实现 |

对照 PRD 和前端页面，**还没实现的有**：

C端模块1中缺失的：
- **个人信息修改** — 修改昵称、头像、城市
- **修改密码** — 旧密码验证 + 新密码 BCrypt 加密更新
- **退出登录** — 虽然前端直接清 localStorage，但后端也应该有个登出接口（清 Redis 缓存）

另外前端有些功能依赖的数据也还没后端接口：
- **个人中心**的「我的发布」— 这个后面做灵感服务时自然会覆盖
- **"记住账号"** — 这个是前端行为，不需要后端接口
- **"忘记密码"** — PRD 里没定义，可以先不做


.m2 缓存里有文件锁残留。你需要在终端里跑一下这条命令清除锁定：

```bash
sudo chflags -R nouchg ~/.m2/repository/com/inspire/platform
mvn clean install -DskipTests
```

如果还不行，试试不用 install，直接用 `package` 编译后跑：

```bash
rm -rf ~/.m2/repository/com/inspire/platform
mvn compile -DskipTests
```

---

## 新增接口清单

| 方法 | 路径 | 说明 | 需要Token |
|------|------|------|----------|
| **原有** | | | |
| POST | `/api/auth/register` | 注册（**新增必填 email**） | 否 |
| POST | `/api/auth/login` | 登录 | 否 |
| GET | `/api/auth/userinfo` | 查个人信息 | 是 |
| **新增** | | | |
| PUT | `/api/auth/userinfo` | 修改信息（昵称/头像/城市） | 是 |
| PUT | `/api/auth/password` | 修改密码（旧+新） | 是 |
| POST | `/api/auth/logout` | 退出登录 | 否（有 token 则清缓存） |
| POST | `/api/auth/forgot-password` | 忘记密码（发重置邮件） | 否 |
| POST | `/api/auth/reset-password` | 重置密码（token+新密码） | 否 |

**数据库变化：** `user` 表加了 `email` 字段，新增 `password_reset` 令牌表


## Swagger 接入

**Auth 服务 pom.xml** 添加了：
```xml
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
    <version>2.5.0</version>
</dependency>
```

`SwaggerConfig.java` 已创建，配置了 API 文档标题和描述。

**访问地址：**
- 直连 auth 服务：`http://localhost:8081/swagger-ui/index.html`
- 通过网关：`http://localhost:8080/swagger-ui/index.html`（已添加路由 + 白名单）

---

## 全部接口测试用例

先把两个服务启动起来：

```bash
cd "/Users/lee/Desktop/Project/Study Project/Codex/inspire-ai-preview/backend"

# 终端 1 - 网关
mvn spring-boot:run -pl inspire-gateway

# 终端 2 - auth 服务
mvn spring-boot:run -pl inspire-auth
```

### 测试 1：注册（必填邮箱）

```bash
curl -s -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"alice","password":"123456","confirmPassword":"123456","email":"alice@test.com"}'
```

预期返回：
```json
{"code":200,"msg":"注册成功","data":{"token":"eyJ...","userId":...,"username":"alice"}}
```

验证异常情况：
```bash
# 重复用户名
curl -s -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"alice","password":"123456","confirmPassword":"123456","email":"alice2@test.com"}'
# → {"code":500,"msg":"用户名已被注册"}
```

```bash
# 两次密码不一致
curl -s -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"bob","password":"123456","confirmPassword":"654321","email":"bob@test.com"}'
# → {"code":500,"msg":"两次密码输入不一致"}
```

```bash
# 缺少邮箱
curl -s -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"bob","password":"123456","confirmPassword":"123456"}'
# → {"code":400,"msg":"email: 邮箱不能为空"}
```

### 测试 2：登录

```bash
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"alice","password":"123456"}' | python3 -c "import sys,json; print(json.load(sys.stdin)['data']['token'])")

echo "TOKEN=$TOKEN"
```

预期：输出的 TOKEN 是 JWT 字符串

### 测试 3：获取用户信息（需 Token）

```bash
curl -s http://localhost:8080/api/auth/userinfo \
  -H "Authorization: Bearer $TOKEN"
```

预期：
```json
{"code":200,"msg":"操作成功","data":{"id":...,"username":"alice","email":"alice@test.com",...}}
```

### 测试 4：修改个人信息

```bash
curl -s -X PUT http://localhost:8080/api/auth/userinfo \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"nickname":"Alice魔法师","city":"上海","avatar":"https://example.com/avatar.png"}'
```

预期：`{"code":200,"msg":"更新成功","data":{...}}`

### 测试 5：修改密码

```bash
curl -s -X PUT http://localhost:8080/api/auth/password \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"oldPassword":"123456","newPassword":"654321"}'
```

预期：`{"code":200,"msg":"密码修改成功","data":null}`

```bash
# 验证旧密码不能再登录
curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"alice","password":"123456"}'
# → {"code":500,"msg":"账号或密码错误"}
```

```bash
# 用新密码登录
curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"alice","password":"654321"}'
# → {"code":200,...}
```

### 测试 6：退出登录

```bash
curl -s -X POST http://localhost:8080/api/auth/logout \
  -H "Authorization: Bearer $TOKEN"
```

预期：`{"code":200,"msg":"已退出登录","data":null}`

### 测试 7：忘记密码（发重置邮件）

```bash
curl -s -X POST http://localhost:8080/api/auth/forgot-password \
  -H "Content-Type: application/json" \
  -d '{"email":"alice@test.com"}'
```

预期：`{"code":200,"msg":"重置链接已发送至您的邮箱，请查收","data":null}`

可以在 auth 服务的终端日志里看到打印的 token：
```
==============================================
收件人: alice@test.com
主题: 【灵思集】密码重置通知
重置链接: http://localhost:8080/reset-password?token=xxxxxxxxxxxx
==============================================
```

### 测试 8：重置密码（用 token）

拿到上面的 token 后执行：

```bash
curl -s -X POST http://localhost:8080/api/auth/reset-password \
  -H "Content-Type: application/json" \
  -d '{"token":"从日志里拿到的token","newPassword":"abcdef"}'
```

预期：`{"code":200,"msg":"密码重置成功","data":null}`

```bash
# 验证新密码能登录
curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"alice","password":"abcdef"}'
# → {"code":200,...}
```

### 测试 9：JWT 令牌过期校验

```bash
# 不用 token 访问需要登录的接口
curl -s http://localhost:8080/api/auth/userinfo
# → {"code":401,"msg":"未登录，请先登录账号"}
```

```bash
# 用错误的 token
curl -s http://localhost:8080/api/auth/userinfo \
  -H "Authorization: Bearer fake.token.here"
# → {"code":401,"msg":"登录已过期，请重新登录"}
```

### 测试 10：Swagger UI

打开浏览器访问：
- `http://localhost:8081/swagger-ui/index.html`
- 或 `http://localhost:8080/swagger-ui/index.html`

可以看到所有 8 个接口的 OpenAPI 文档，支持在线测试。

---

要编译的话，如果 `.m2` 还有锁的问题，可以试试：

```bash
sudo rm -rf ~/.m2/repository/com/inspire/platform
mvn clean install -DskipTests
```

