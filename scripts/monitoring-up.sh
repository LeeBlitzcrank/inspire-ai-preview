#!/usr/bin/env bash
#
# 文件：scripts/monitoring-up.sh
# 所属模块：本地开发和运维脚本
# 主要职责：Shell 脚本，封装本地开发或运维命令
# 维护说明：注释解释文件边界和核心意图，具体业务规则以方法、组件和主文档说明为准。
# INSPIRE_FILE_HEADER
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

docker compose --env-file .env --profile monitoring up -d prometheus grafana

echo "Prometheus: http://127.0.0.1:9090"
echo "Grafana:    http://127.0.0.1:3000"
echo "Grafana 初始账号: ${GRAFANA_ADMIN_USER:-admin} / ${GRAFANA_ADMIN_PASSWORD:-admin}"
