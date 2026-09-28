# Inspire AI Documentation Index

> 更新时间：2026-09-28
> 规则：当前实现只以本索引中的“当前主文档”和代码为准；归档文档仅用于追溯历史，不得作为当前开发依据。

## 1. 项目入口

| 内容 | 当前文档 |
| --- | --- |
| 项目说明与启动 | [README.md](../README.md) |
| AI 开发规范 | [AGENTS.md](../AGENTS.md) |
| 项目总览 | [Inspire AI Preview 项目总览文档.md](current/Inspire%20AI%20Preview%20项目总览文档.md) |
| 产品需求 | [PRD.md](current/PRD.md) |

## 2. 架构与数据库

| 内容 | 当前文档 |
| --- | --- |
| 数据库文件职责 | [database/README.md](../database/README.md) |
| 数据库 v2 设计与对比 | [数据库v2重构设计与对比.md](current/数据库v2重构设计与对比.md) |
| 数据库扩容与监控 | [数据库扩容与运行监控.md](current/数据库扩容与运行监控.md) |
| 网关与中间件 | [middlewareDoc.md](current/middlewareDoc.md) |
| 鉴权体系 | [Inspire AI 鉴权体系完整说明.md](current/Inspire%20AI%20鉴权体系完整说明.md) |

数据库运行时文件：

```text
database/init/init.sql
backend/inspire-core/src/main/resources/db/migration/V*.sql
```

## 3. 安全

| 内容 | 当前文档 |
| --- | --- |
| 业务、安全与性能审计 | [项目业务安全与性能审计报告.md](current/项目业务安全与性能审计报告.md) |
| 密钥泄露与 Git 清理 | [密钥泄露与Git清理清单.md](current/密钥泄露与Git清理清单.md) |

## 4. 性能与可观测性

| 内容 | 当前文档 |
| --- | --- |
| 前端性能优化 | [前端性能优化记录.md](current/前端性能优化记录.md) |
| 前端组件职责拆分 | [前端组件重构说明.md](current/前端组件重构说明.md) |
| 可观测性与测试 | [可观测性与安全并发测试说明.md](current/可观测性与安全并发测试说明.md) |
| 数据库扩容与监控 | [数据库扩容与运行监控.md](current/数据库扩容与运行监控.md) |
| 生产优化操作 | [生产性能优化操作清单.md](current/生产性能优化操作清单.md) |

历史压测报告：

```text
docs/current/性能压测问题与优化报告.md
```

该报告保留当时的历史结论，当前状态以审计报告、可观测性文档和数据库扩容文档为准。

## 5. 测试

| 内容 | 当前文档 |
| --- | --- |
| 测试与可观测性总览 | [可观测性与安全并发测试说明.md](current/可观测性与安全并发测试说明.md) |
| Web Vitals 与冒烟测试 | [Playwright冒烟测试与WebVitals说明.md](current/Playwright冒烟测试与WebVitals说明.md) |

测试入口：

```text
backend/inspire-common/src/test/
backend/inspire-core/src/test/
frontend/tests/e2e/
```

## 6. 功能说明

| 内容 | 当前文档 |
| --- | --- |
| AI 创作与素材 | [AI创作增强与素材管理说明.md](current/AI创作增强与素材管理说明.md) |
| 多模态 RAG | [多模态RAG落地说明.md](current/多模态RAG落地说明.md) |
| 世界种子与平行剧情 | [世界种子功能说明.md](current/世界种子功能说明.md) |
| 互动与私信 | [互动增强与私信功能说明.md](current/互动增强与私信功能说明.md) |
| 私信、搜索、详情改版 | [私信-搜索-详情页改版说明.md](current/私信-搜索-详情页改版说明.md) |
| 全局错误弹窗 | [全局错误弹窗组件说明.md](current/全局错误弹窗组件说明.md) |
| 设计系统 | [设计系统与主题切换.md](current/设计系统与主题切换.md) |

## 7. 运维

| 内容 | 当前文档 |
| --- | --- |
| 生产优化操作 | [生产性能优化操作清单.md](current/生产性能优化操作清单.md) |
| Docker 常用操作 | [docker.md](current/project%20Tool/docker.md) |
| MySQL/Redis 备份 | [mysql、redis持久化备份.md](current/project%20Tool/mysql、redis持久化备份.md) |
| RocketMQ 部署 | [rocketmq-deploy.md](current/rocketmq-deploy.md) |

## 8. 归档文档

以下内容已经归档，只保留历史追溯价值：

```text
docs/archive/database/database-schema-v1.md
docs/archive/database/sqlDoc-v1.md
docs/archive/planning/projectDev.md
docs/archive/planning/projectProblem.md
docs/archive/scratch/legacy-notes.txt
docs/archive/scratch/legacy-ai-summary.txt
docs/archive/progress/
```

不再作为当前实现依据的典型主题：

- v1 手工分表 SQL
- 早期 35 表结构说明
- 早期项目排期
- 旧架构评审
- 历史预览稿和临时命令
