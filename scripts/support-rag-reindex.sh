#!/usr/bin/env bash
#
# 文件：scripts/support-rag-reindex.sh
# 所属模块：本地开发和运维脚本
# 主要职责：重建独立项目客服知识库索引
# INSPIRE_FILE_HEADER
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
ENV_FILE="${INSPIRE_ENV_FILE:-$ROOT_DIR/.env}"
RAG_URL="${INSPIRE_RAG_INTERNAL_URL:-http://127.0.0.1:8087}"
TOKEN="$(awk -F= '/^INSPIRE_RAG_ADMIN_TOKEN=/{print $2}' "$ENV_FILE" 2>/dev/null | tail -1)"

if [[ -z "$TOKEN" ]]; then
  echo "未配置 INSPIRE_RAG_ADMIN_TOKEN，拒绝调用内部重建接口。" >&2
  exit 1
fi

echo "==> 重建项目客服知识库"
curl -sS -m 1800 -X POST "$RAG_URL/rag/support/admin/reindex" \
  -H "X-Rag-Token: $TOKEN" | jq .
