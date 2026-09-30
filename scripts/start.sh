#!/usr/bin/env bash
#
# 文件：scripts/start.sh
# 所属模块：本地开发和运维脚本
# 主要职责：Shell 脚本，封装本地开发或运维命令
# 维护说明：注释解释文件边界和核心意图，具体业务规则以方法、组件和主文档说明为准。
# INSPIRE_FILE_HEADER
# =============================================
# 启动全部服务 —— 保留已有数据库与缓存
# 用途：日常重启，MySQL / Elasticsearch / MinIO 图片 / Redis 数据都保留
# =============================================
set -e
ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
ENV_FILE="${INSPIRE_ENV_FILE:-.env}"
cd "$ROOT_DIR"
source "$ROOT_DIR/scripts/lib/docker.sh"
source "$ROOT_DIR/scripts/lib/ollama.sh"

# 仅从项目 env 文件读取该密钥，避免旧 shell 导出值覆盖新配置。
unset INSPIRE_UNSPLASH_ACCESS_KEY

# 0. 先确保 Docker daemon 可用，避免构建完成后才发现无法启动容器。
ensure_docker_running
ensure_ollama_ready "$ENV_FILE"

# 1. 构建后端 JAR（Dockerfile.local 直接 COPY 各模块 target/*.jar）
if [[ "${SKIP_BUILD:-0}" != "1" ]]; then
  echo "==> 构建后端 JAR …"
  export JAVA_HOME="${JAVA_HOME:-/Users/lee/Library/Java/JavaVirtualMachines/jbr-21.0.8-1/Contents/Home}"
  (cd backend && mvn package \
      -pl inspire-common,inspire-mq,inspire-gateway,inspire-auth,inspire-core,inspire-admin,inspire-ai,inspire-search,inspire-rag \
      -am -DskipTests)
fi

echo "==> 启动容器（保留数据卷）…"
docker compose --env-file "$ENV_FILE" up -d --build --wait --wait-timeout 240
bash "$ROOT_DIR/deploy/minio/init-public-policy.sh"
bash "$ROOT_DIR/deploy/cloudflare/start-tunnel.sh"
docker compose --env-file "$ENV_FILE" ps

# 前端：重新打包 +（重新）启动 dev server
if [[ "${SKIP_FRONTEND:-0}" != "1" ]]; then
  echo "==> 构建并启动前端 …"
  bash "$ROOT_DIR/scripts/frontend-up.sh"
else
  echo "==> 已跳过前端（SKIP_FRONTEND=1）"
fi

echo "==> 完成。"
