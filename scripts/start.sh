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
# 默认：复用已有服务镜像，Maven 构建后把最新 JAR 同步到容器
# REBUILD_IMAGES=1：完整重新构建 Docker 镜像（依赖基础镜像可访问）
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

SERVICES=(
  inspire-auth
  inspire-ai
  inspire-core
  inspire-admin
  inspire-search
  inspire-gateway
  inspire-rag
)

module_for_service() {
  case "$1" in
    inspire-auth|inspire-ai|inspire-core|inspire-admin|inspire-search|inspire-gateway|inspire-rag)
      echo "$1"
      ;;
    *)
      echo ""
      ;;
  esac
}

sync_runtime_jars() {
  local service module jar
  for service in "${SERVICES[@]}"; do
    module="$(module_for_service "$service")"
    jar="backend/${module}/target/${module}-1.0.0.jar"
    if [[ -f "$jar" ]] && docker inspect "$service" >/dev/null 2>&1; then
      echo "==> 同步 JAR: ${service}"
      docker cp "$jar" "${service}:/app/app.jar"
    fi
  done
  docker compose --env-file "$ENV_FILE" restart "${SERVICES[@]}"
}

wait_for_runtime() {
  local waited=0 timeout="${RUNTIME_WAIT_TIMEOUT:-180}" service state
  while (( waited < timeout )); do
    local ready=1
    for service in "${SERVICES[@]}"; do
      state="$(docker inspect -f '{{if .State.Health}}{{.State.Health.Status}}{{else}}{{.State.Status}}{{end}}' "$service" 2>/dev/null || echo missing)"
      if [[ "$state" != "healthy" && "$state" != "running" ]]; then
        ready=0
        break
      fi
    done
    if (( ready == 1 )); then
      echo "==> 后端容器已就绪"
      return 0
    fi
    sleep 3
    waited=$((waited + 3))
  done
  echo "错误：后端容器未在 ${timeout}s 内就绪。"
  docker compose --env-file "$ENV_FILE" ps
  return 1
}

echo "==> 启动容器（保留数据卷）…"
if [[ "${REBUILD_IMAGES:-0}" == "1" ]]; then
  echo "==> REBUILD_IMAGES=1，重新构建服务镜像…"
  docker compose --env-file "$ENV_FILE" up -d --build --wait --wait-timeout 240
else
  if ! docker compose --env-file "$ENV_FILE" up -d --no-build --wait --wait-timeout 240; then
    echo "==> 已有镜像不可用，回退到完整镜像构建…"
    docker compose --env-file "$ENV_FILE" up -d --build --wait --wait-timeout 240
  else
    sync_runtime_jars
    wait_for_runtime
  fi
fi
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
