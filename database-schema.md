# 数据库表结构文档

> 数据库: `inspire_ai_preview` | 表总数: 35 | 引擎: InnoDB | 字符集: utf8mb4

---

## 一、灵感核心（3 表）

### `inspire_main` — 灵感主表

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | 主键 |
| title | VARCHAR(120) | 灵感标题 |
| img | VARCHAR(255) | 封面图 URL |
| images | JSON | 多图列表 |
| tag | VARCHAR(30) | 分类 |
| user_id | BIGINT | 发布人 ID → `user.id` |
| status | TINYINT | 0=草稿 1=已发布 2=待审核 |
| view_count / like_count / collect_count / heat / share_count | INT/BIGINT | 计数统计 |
| publish_city | VARCHAR(32) | 发布城市 |
| create_time / update_time | DATETIME | 时间戳 |
| deleted | TINYINT | 逻辑删除 |
| ext_json | JSON | 扩展字段 |

**索引：** `idx_tag_deleted(tag, deleted)`, `idx_user_id(user_id)`, `idx_status_time(status, create_time)`

### `inspire_content` — 灵感正文

| 字段 | 类型 | 说明 |
|------|------|------|
| inspire_id | BIGINT PK | 灵感 ID → `inspire_main.id` |
| content | TEXT | Markdown 正文 |
| create_time / update_time | DATETIME | 时间戳 |

### `inspire_version` — 版本历史

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | 主键 |
| inspire_id | BIGINT | 灵感 ID → `inspire_main.id` |
| version_number | INT | 版本号（递增） |
| title | VARCHAR(120) | 标题快照 |
| content | TEXT | 正文快照 |
| img / images / tag | — | 其他字段快照 |
| change_summary | VARCHAR(500) | 变更说明 |
| create_time | DATETIME | 创建时间 |

---

## 二、收藏（11 表）

### `collect_folder` — 收藏文件夹

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | 主键 |
| user_id | BIGINT | 所属用户 → `user.id` |
| name | VARCHAR(50) | 文件夹名称 |
| icon | VARCHAR(10) | 图标 emoji |
| sort_order | INT | 排序 |

### `collect_0` ~ `collect_9` — 收藏分表（按 user_id % 10 路由）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | 主键 |
| user_id | BIGINT | 用户 ID |
| inspire_id | BIGINT | 灵感 ID → `inspire_main.id` |
| folder_id | BIGINT | 文件夹 ID → `collect_folder.id` |
| create_time | DATETIME | 收藏时间 |

**唯一约束：** `uk_user_inspire(user_id, inspire_id)`

---

## 三、点赞（10 表）

### `inspire_like_0` ~ `inspire_like_9` — 点赞分表（按 inspire_id % 10 路由）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | 主键 |
| user_id | BIGINT | 用户 ID → `user.id` |
| inspire_id | BIGINT | 灵感 ID → `inspire_main.id` |
| create_time | DATETIME | 点赞时间 |

**唯一约束：** `uk_inspire_user(inspire_id, user_id)`

---

## 四、用户与认证（2 表）

### `user` — 用户表

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | 主键 |
| username | VARCHAR(50) | 登录名（唯一） |
| password | VARCHAR(255) | BCrypt 加密密码 |
| nickname | VARCHAR(50) | 昵称 |
| avatar | VARCHAR(255) | 头像 |
| email | VARCHAR(100) | 邮箱 |
| city | VARCHAR(32) | 城市 |
| create_time / update_time | DATETIME | 时间戳 |

### `password_reset` — 密码重置令牌

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | 主键 |
| user_id | BIGINT | 用户 ID → `user.id` |
| token | VARCHAR(255) | 重置令牌（唯一） |
| used | TINYINT | 0=未使用 1=已使用 |
| create_time | DATETIME | 创建时间 |

---

## 五、评论（1 表）

### `inspire_comment` — 灵感评论

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | 主键 |
| inspire_id | BIGINT | 灵感 ID → `inspire_main.id` |
| user_id | BIGINT | 评论人 ID → `user.id` |
| username | VARCHAR(60) | 评论人昵称 |
| parent_id | BIGINT | 回复目标评论 ID（0=顶级） |
| reply_user_id | BIGINT | 被回复用户 ID |
| content | VARCHAR(500) | 评论内容 |
| create_time / update_time | DATETIME | 时间戳 |
| deleted | TINYINT | 逻辑删除 |

---

## 六、关注（1 表）

### `user_follow` — 用户关注

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | 主键 |
| follower_id | BIGINT | 关注者 → `user.id` |
| following_id | BIGINT | 被关注者 → `user.id` |
| create_time | DATETIME | 关注时间 |

**唯一约束：** `uk_follow(follower_id, following_id)`

---

## 七、消息（2 表）

### `message_conversation` — 会话

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | 主键 |
| user_id_1 | BIGINT | 参与者 1 → `user.id` |
| user_id_2 | BIGINT | 参与者 2 → `user.id` |
| last_message | TEXT | 最后一条消息摘要 |
| unread_count_1 / unread_count_2 | INT | 未读数 |
| create_time / update_time | DATETIME | 时间戳 |

### `message` — 消息内容

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | 主键 |
| conversation_id | BIGINT | 会话 ID → `message_conversation.id` |
| sender_id | BIGINT | 发送者 → `user.id` |
| content | TEXT | 消息内容 |
| create_time | DATETIME | 发送时间 |

---

## 八、通知（1 表）

### `user_notification` — 用户通知

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | 主键 |
| user_id | BIGINT | 接收者 → `user.id` |
| type | VARCHAR(20) | 通知类型: collect/like/comment |
| actor_id | BIGINT | 触发者 → `user.id` |
| actor_name | VARCHAR(60) | 触发者昵称 |
| content | VARCHAR(200) | 通知内容 |
| target_id | BIGINT | 目标灵感 ID |
| target_title | VARCHAR(120) | 目标标题 |
| is_read | TINYINT | 0=未读 1=已读 |
| deleted | TINYINT | 逻辑删除 |
| create_time | DATETIME | 创建时间 |

---

## 九、AI（1 表）

### `ai_call_log` — AI 调用记录

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | 主键 |
| call_date | DATE | 调用日期 |
| keyword | VARCHAR(100) | 关键词 |
| user_id | BIGINT | 用户 ID |
| result | TEXT | 返回结果 |
| create_time | DATETIME | 调用时间 |

---

## 十、管理后台（2 表）

### `admin_user` — 管理员

| 字段 | 类型 | 说明 |
|------|------|------|
| id | INT AUTO_INCREMENT PK | 主键 |
| username | VARCHAR(50) | 账号（唯一） |
| password | VARCHAR(100) | BCrypt 密码 |
| nickname | VARCHAR(50) | 昵称 |
| create_time | DATETIME | 创建时间 |

### `admin_config` — 运营配置

| 字段 | 类型 | 说明 |
|------|------|------|
| id | INT AUTO_INCREMENT PK | 主键 |
| config_key | VARCHAR(50) | 配置键（唯一） |
| config_value | VARCHAR(500) | 配置值 |
| desc | VARCHAR(120) | 说明 |
| create_time / update_time | DATETIME | 时间戳 |

---

## 十一、表关系图（ER）

```
user ──┬── inspire_main ──┬── inspire_content
       │                  ├── inspire_version
       │                  ├── collect_0~9 ── collect_folder
       │                  ├── inspire_like_0~9
       │                  ├── inspire_comment
       │                  └── user_notification
       │
       ├── user_follow (follower_id → user.id)
       │              (following_id → user.id)
       ├── message_conversation ── message
       └── password_reset
```

### 核心流程

```
用户上传灵感 → inspire_main + inspire_content
        ↓
  其他用户操作：
  ├── 收藏 → collect_{id%10} + collect_count++
  ├── 点赞 → inspire_like_{id%10} + like_count++
  ├── 评论 → inspire_comment
  ├── 关注 → user_follow
  └── 通知 → user_notification

AI 调用 → ai_call_log

编辑灵感 → inspire_version（保存旧快照）
```

---

## 十二、建表脚本

建表脚本统一在 `docker/init/init.sql` 中，包含全部 35 张表的 `CREATE TABLE IF NOT EXISTS` 语句。

本地开发如需单独建表，可运行对应模块的 `src/main/resources/schema.sql`。
