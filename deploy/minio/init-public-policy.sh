#!/usr/bin/env bash
#
# 文件：deploy/minio/init-public-policy.sh
# 所属模块：MinIO、Nginx 和对象存储策略
# 主要职责：Shell 脚本，封装本地开发或运维命令
# 维护说明：注释解释文件边界和核心意图，具体业务规则以方法、组件和主文档说明为准。
# INSPIRE_FILE_HEADER
set -e

ROOT_DIR="$(cd "$(dirname "$0")/../.." && pwd)"
POLICY_FILE="$ROOT_DIR/deploy/minio/inspire-img-policy.json"

docker cp "$POLICY_FILE" inspire-minio:/tmp/inspire-img-policy.json
docker exec inspire-minio sh -c 'mc alias set local http://localhost:9000 "$MINIO_ROOT_USER" "$MINIO_ROOT_PASSWORD"' >/dev/null
docker exec inspire-minio mc anonymous set-json /tmp/inspire-img-policy.json local/inspire-img
