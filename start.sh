#!/usr/bin/env bash
# =============================================
# 启动全部服务 —— 保留已有数据库与缓存
# 用途：日常重启，MySQL / Elasticsearch / MinIO 图片 / Redis 数据都保留
# =============================================
set -e
cd "$(dirname "$0")"

# 1. 构建后端 JAR（Dockerfile.local 直接 COPY 各模块 target/*.jar）
if [[ "${SKIP_BUILD:-0}" != "1" ]]; then
  echo "==> 构建后端 JAR …"
  export JAVA_HOME="${JAVA_HOME:-/Users/lee/Library/Java/JavaVirtualMachines/jbr-21.0.8-1/Contents/Home}"
  (cd backend && mvn package \
      -pl inspire-common,inspire-mq,inspire-gateway,inspire-auth,inspire-core,inspire-admin,inspire-ai,inspire-search \
      -am -DskipTests)
fi

echo "==> 启动容器（保留数据卷）…"
docker compose up -d --build
docker compose ps

# 前端：重新打包 +（重新）启动 dev server
if [[ "${SKIP_FRONTEND:-0}" != "1" ]]; then
  echo "==> 构建并启动前端 …"
  bash "$(dirname "$0")/frontend-up.sh"
else
  echo "==> 已跳过前端（SKIP_FRONTEND=1）"
fi

echo "==> 完成。"
