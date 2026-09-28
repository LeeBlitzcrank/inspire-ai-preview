# Database Source of Truth

> 更新时间：2026-09-28

数据库文件必须按以下职责使用，不要继续新增重复的建表副本。

## 1. 全新空库初始化

唯一入口：

```text
database/init/init.sql
```

职责：

- MySQL 容器第一次创建数据卷时执行。
- 只负责创建当前 v2 基线结构。
- 不承载后续业务字段变更。
- 修改结构后，新的变更必须进入 Flyway。

## 2. 后续结构迁移

唯一入口：

```text
backend/inspire-core/src/main/resources/db/migration/
```

当前版本：

```text
V1__baseline_v2.sql
V2__message_recall_audit.sql
V3__admin_totp_mfa.sql
V4__user_feed_inbox.sql
V5__metrics_and_archives.sql
```

职责：

- 所有后续字段、索引、表结构变化。
- 每次变化新增一个已版本化 SQL 文件。
- 已执行过的迁移文件不得修改，只能追加新版本。

## 3. 当前结构设计说明

当前有效文档：

```text
docs/current/数据库v2重构设计与对比.md
docs/current/数据库扩容与运行监控.md
```

## 4. 历史文档

旧版 SQL 和 v1 结构文档已归档：

```text
docs/archive/database/database-schema-v1.md
docs/archive/database/sqlDoc-v1.md
```

归档文件只用于追溯，不得作为当前建表或迁移依据。

## 5. 目录与模块边界

```text
database/
├── README.md
└── init/
    └── init.sql

backend/inspire-core/src/main/resources/db/migration/
└── V*.sql
```

Flyway 迁移文件保留在 `inspire-core` 的 classpath 中，这是运行时加载要求，不属于目录遗漏。不要为了统一目录而把迁移文件移到 classpath 之外。

图片上传元数据表 `sys_upload_image` 已并入 `database/init/init.sql`。旧的独立 `schema-minio.sql` 是重复建表副本，已移除，后续只维护基线或 Flyway 迁移。
