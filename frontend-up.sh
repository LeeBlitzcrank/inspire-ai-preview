#!/usr/bin/env bash
# =============================================
# 前端：重新打包 + 重启开发服务（被 start.sh / reset-data.sh 调用）
# 可用 SKIP_FRONTEND=1 跳过；改端口用 FRONTEND_PORT=xxxx
# =============================================
set -e

ROOT="$(cd "$(dirname "$0")" && pwd)"
PORT="${FRONTEND_PORT:-5173}"
LOG="$ROOT/log/frontend-dev.log"

cd "$ROOT/frontend"

if [ ! -d node_modules ]; then
  echo "==> 安装前端依赖（首次较慢）…"
  npm install
fi

echo "==> 构建前端 …"
npm run build

# 关掉旧的 dev server，保证跑的是刚构建的代码
OLD_PID="$(lsof -nP -iTCP:$PORT -sTCP:LISTEN -t 2>/dev/null | head -1 || true)"
if [ -n "$OLD_PID" ]; then
  echo "==> 停止旧的 dev server (pid=$OLD_PID) …"
  kill "$OLD_PID" 2>/dev/null || true
  sleep 1
fi

mkdir -p "$ROOT/log"
echo "==> 启动前端 dev server …"
nohup npm run dev -- --host 127.0.0.1 --port "$PORT" > "$LOG" 2>&1 &
# 脱离当前 shell 的作业表，避免脚本退出时被连带回收
disown 2>/dev/null || true

# 等它起来（最多 15 秒）
for i in $(seq 1 15); do
  if lsof -nP -iTCP:$PORT -sTCP:LISTEN >/dev/null 2>&1; then
    echo "==> 前端已就绪：http://127.0.0.1:$PORT   （日志：log/frontend-dev.log）"
    exit 0
  fi
  sleep 1
done

echo "⚠️  前端未在 $PORT 起来，请看 log/frontend-dev.log"
exit 1
