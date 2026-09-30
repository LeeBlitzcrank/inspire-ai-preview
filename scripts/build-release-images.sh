#!/usr/bin/env bash
#
# 文件：scripts/build-release-images.sh
# 所属模块：本地开发和运维脚本
# 主要职责：Shell 脚本，封装本地开发或运维命令
# 维护说明：注释解释文件边界和核心意图，具体业务规则以方法、组件和主文档说明为准。
# INSPIRE_FILE_HEADER
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

REGISTRY="${IMAGE_REGISTRY:-}"
TAG="${IMAGE_TAG:-$(git rev-parse --short HEAD 2>/dev/null || date +%Y%m%d%H%M%S)}"
PUSH="${PUSH_IMAGES:-0}"
SERVICES=(
  inspire-auth
  inspire-ai
  inspire-core
  inspire-admin
  inspire-search
  inspire-gateway
  inspire-rag
)

MODULES="$(IFS=,; echo "${SERVICES[*]}")"
echo "构建后端 JAR: ${MODULES}"
(
  cd backend
  mvn package -DskipTests -pl "$MODULES" -am
)

for service in "${SERVICES[@]}"; do
  if [[ -n "$REGISTRY" ]]; then
    image="${REGISTRY%/}/${service}:${TAG}"
  else
    image="inspire-ai-preview/${service}:${TAG}"
  fi
  echo "构建镜像: ${image}"
  docker build \
    -f deploy/docker/Dockerfile.local \
    --build-arg "BUILD_MODULE=${service}" \
    -t "$image" \
    .
  if [[ "$PUSH" == "1" ]]; then
    echo "推送镜像: ${image}"
    docker push "$image"
  fi
done

echo "镜像构建完成，标签: ${TAG}"
