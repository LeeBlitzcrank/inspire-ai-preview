#!/usr/bin/env bash
#
# 文件：scripts/frontend-up.sh
# 所属模块：本地开发和运维脚本
# 主要职责：Shell 脚本，封装本地开发或运维命令
# 维护说明：注释解释文件边界和核心意图，具体业务规则以方法、组件和主文档说明为准。
# INSPIRE_FILE_HEADER
# =============================================
# 前端：重新打包 + 重启开发服务（被 scripts/start.sh / scripts/reset-data.sh 调用）
# 可用 SKIP_FRONTEND=1 跳过；改端口用 FRONTEND_PORT=xxxx
# =============================================
set -e

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
PORT="${FRONTEND_PORT:-5173}"
LOG="$ROOT/log/frontend-dev.log"
NPM_BIN="$(command -v npm 2>/dev/null || true)"

if [[ -z "$NPM_BIN" ]]; then
  echo "错误：未找到 npm，请先安装 Node.js 或加载 nvm。" >&2
  exit 1
fi

cd "$ROOT/frontend"

if [ ! -d node_modules ]; then
  echo "==> 安装前端依赖（首次较慢）…"
  "$NPM_BIN" install
fi

echo "==> 构建前端 …"
"$NPM_BIN" run build

# 关掉旧的 dev server，保证跑的是刚构建的代码
OLD_PID="$(lsof -nP -iTCP:$PORT -sTCP:LISTEN -t 2>/dev/null | head -1 || true)"
if [ -n "$OLD_PID" ]; then
  echo "==> 停止旧的 dev server (pid=$OLD_PID) …"
  kill "$OLD_PID" 2>/dev/null || true
  sleep 1
fi

if [ "$(uname -s)" = "Darwin" ] && command -v launchctl >/dev/null 2>&1; then
  bash "$ROOT/scripts/install-frontend-launch-agent.sh"
  exit 0
fi

mkdir -p "$ROOT/log"
echo "==> 启动前端 dev server …"
nohup "$NPM_BIN" run dev -- --host 127.0.0.1 --port "$PORT" </dev/null > "$LOG" 2>&1 &
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
