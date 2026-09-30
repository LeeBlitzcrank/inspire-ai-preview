#!/usr/bin/env bash
#
# 文件：scripts/lib/ollama.sh
# 所属模块：本地开发和运维脚本
# 主要职责：Shell 脚本，封装本地开发或运维命令
# 维护说明：注释解释文件边界和核心意图，具体业务规则以方法、组件和主文档说明为准。
# INSPIRE_FILE_HEADER

ensure_ollama_ready() {
  local env_file="$1"
  local enabled model url
  enabled="$(awk -F= '/^INSPIRE_RAG_OLLAMA_EMBEDDING_ENABLED=/{print $2}' "$env_file" 2>/dev/null | tail -1)"
  model="$(awk -F= '/^INSPIRE_RAG_EMBEDDING_MODEL=/{print $2}' "$env_file" 2>/dev/null | tail -1)"
  url="$(awk -F= '/^INSPIRE_RAG_OLLAMA_URL=/{print $2}' "$env_file" 2>/dev/null | tail -1)"
  model="${model:-embeddinggemma:300m}"
  url="${url:-http://127.0.0.1:11434}"

  if [[ "$enabled" != "true" ]]; then
    return 0
  fi

  if [[ "$url" == *"host.docker.internal"* ]]; then
    url="http://127.0.0.1:11434"
  fi

  if ! curl -sS --max-time 2 "$url/api/tags" >/dev/null 2>&1; then
    if [[ "$(uname -s)" == "Darwin" ]] && [[ -d "/Applications/Ollama.app" ]]; then
      echo "==> Ollama 未运行，正在启动…"
      open -a Ollama >/dev/null 2>&1 || true
    fi
  fi

  local i
  for i in $(seq 1 30); do
    if curl -sS --max-time 2 "$url/api/tags" >/dev/null 2>&1; then
      if curl -sS --max-time 5 "$url/api/tags" | grep -q "\"$model\""; then
        echo "==> Ollama 已就绪: $model"
        return 0
      fi
      echo "警告：Ollama 已运行，但未找到模型 $model，请执行：ollama pull $model"
      return 0
    fi
    sleep 1
  done

  echo "警告：Ollama 未能在 30 秒内启动，RAG 将降级为本地哈希向量。"
  return 0
}
