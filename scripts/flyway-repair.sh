#!/usr/bin/env bash
#
# 文件：scripts/flyway-repair.sh
# 所属模块：本地开发和运维脚本
# 主要职责：使用官方 Flyway repair 修复历史迁移 checksum
# 维护说明：只修复 flyway_schema_history_core，不修改业务表和数据。
# INSPIRE_FILE_HEADER
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
ENV_FILE="${INSPIRE_ENV_FILE:-$ROOT_DIR/.env}"

if [[ ! -f "$ENV_FILE" ]]; then
  echo "错误：未找到环境文件 $ENV_FILE" >&2
  exit 1
fi

DB_PASSWORD="${INSPIRE_FLYWAY_PASSWORD:-}"
if [[ -z "$DB_PASSWORD" ]]; then
  DB_PASSWORD="$(docker inspect -f '{{range .Config.Env}}{{println .}}{{end}}' inspire-mysql 2>/dev/null \
    | awk -F= '/^MYSQL_ROOT_PASSWORD=/{print $2}' | tail -1)"
fi
if [[ -z "$DB_PASSWORD" ]]; then
  DB_PASSWORD="$(awk -F= '/^INSPIRE_DB_PASSWORD=/{print $2}' "$ENV_FILE" | tail -1)"
fi
if [[ -z "$DB_PASSWORD" ]]; then
  echo "错误：INSPIRE_DB_PASSWORD 未配置" >&2
  exit 1
fi

DB_URL="${INSPIRE_FLYWAY_URL:-jdbc:mysql://127.0.0.1:3307/inspire_ai_preview?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true}"

echo "==> 执行官方 Flyway repair"
(
  cd "$ROOT_DIR/backend"
  JAVA_HOME="${JAVA_HOME:-/Users/lee/Library/Java/JavaVirtualMachines/jbr-21.0.8-1/Contents/Home}" \
  mvn -pl inspire-core flyway:repair \
    -Dflyway.url="$DB_URL" \
    -Dflyway.user="${INSPIRE_FLYWAY_USER:-root}" \
    -Dflyway.password="$DB_PASSWORD"
)

echo "==> 校验迁移历史"
(
  cd "$ROOT_DIR/backend"
  JAVA_HOME="${JAVA_HOME:-/Users/lee/Library/Java/JavaVirtualMachines/jbr-21.0.8-1/Contents/Home}" \
  mvn -pl inspire-core flyway:info \
    -Dflyway.url="$DB_URL" \
    -Dflyway.user="${INSPIRE_FLYWAY_USER:-root}" \
    -Dflyway.password="$DB_PASSWORD"
)
