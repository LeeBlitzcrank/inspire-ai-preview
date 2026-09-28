#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
ENV_FILE="${INSPIRE_ENV_FILE:-$ROOT_DIR/.env}"
LIMIT="${1:-5000}"
RAG_URL="${INSPIRE_RAG_INTERNAL_URL:-http://127.0.0.1:8087}"
TOKEN="$(awk -F= '/^INSPIRE_RAG_ADMIN_TOKEN=/{print $2}' "$ENV_FILE" 2>/dev/null | tail -1)"

if [[ -z "$TOKEN" ]]; then
  echo "未配置 INSPIRE_RAG_ADMIN_TOKEN，拒绝调用内部重建接口。" >&2
  exit 1
fi

if ! command -v jq >/dev/null 2>&1; then
  echo "缺少 jq，无法格式化进度。" >&2
  exit 1
fi

RESULT_FILE="$(mktemp)"
trap 'rm -f "$RESULT_FILE"' EXIT

echo "==> 启动全量 RAG 重建: limit=$LIMIT"
curl -sS -m 7200 -X POST "$RAG_URL/rag/admin/reindex?limit=$LIMIT" \
  -H "X-Rag-Token: $TOKEN" >"$RESULT_FILE" &
REINDEX_PID=$!

while kill -0 "$REINDEX_PID" 2>/dev/null; do
  STATUS="$(curl -sS --max-time 5 "$RAG_URL/rag/admin/reindex/status" \
    -H "X-Rag-Token: $TOKEN" 2>/dev/null || true)"
  if [[ -n "$STATUS" ]]; then
    echo "$STATUS" | jq -r '
      .data |
      "阶段=\(.phase) chunk=\(.processedChunks)/\(.totalChunks) " +
      "灵感=\(.processedDocuments)/\(.totalDocuments) " +
      "进度=\(.progressPercent)% " +
      "预计剩余=\((if .estimatedRemainingMs >= 0 then ((.estimatedRemainingMs / 1000) | floor | tostring) + "秒" else "计算中" end))"'
  fi
  sleep 5
done

wait "$REINDEX_PID"
echo "==> 重建完成"
jq . "$RESULT_FILE"
