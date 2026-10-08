#!/usr/bin/env bash
#
# 文件：scripts/install-frontend-launch-agent.sh
# 所属模块：本地开发和运维脚本
# 主要职责：将 Vite 本地开发服务注册为 macOS 用户级常驻服务
# INSPIRE_FILE_HEADER
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
PORT="${FRONTEND_PORT:-5173}"
LABEL="com.inspire.frontend"
PLIST_DIR="$HOME/Library/LaunchAgents"
PLIST_FILE="$PLIST_DIR/$LABEL.plist"
LOG_DIR="$ROOT_DIR/log"
UID_VALUE="$(id -u)"
NODE_BIN="$(command -v node 2>/dev/null || true)"
NPM_BIN="$(command -v npm 2>/dev/null || true)"
if [[ -z "$NODE_BIN" || -z "$NPM_BIN" ]]; then
  echo "错误：未找到 node/npm，请先安装 Node.js 或加载 nvm。" >&2
  exit 1
fi
NODE_BIN_DIR="$(dirname "$NODE_BIN")"
NPM_BIN_DIR="$(dirname "$NPM_BIN")"
LAUNCH_PATH="$NODE_BIN_DIR:$NPM_BIN_DIR:/usr/local/bin:/usr/bin:/bin:/usr/sbin:/sbin"

if [[ "$(uname -s)" != "Darwin" ]]; then
  echo "当前系统不是 macOS，跳过 LaunchAgent。"
  exit 0
fi

mkdir -p "$PLIST_DIR" "$LOG_DIR"

cat >"$PLIST_FILE" <<EOF
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
<dict>
  <key>Label</key>
  <string>$LABEL</string>
  <key>ProgramArguments</key>
  <array>
    <string>/bin/zsh</string>
    <string>-lc</string>
    <string>cd "$ROOT_DIR/frontend" &amp;&amp; exec "$NPM_BIN" run dev -- --host 127.0.0.1 --port $PORT</string>
  </array>
  <key>WorkingDirectory</key>
  <string>$ROOT_DIR/frontend</string>
  <key>RunAtLoad</key>
  <true/>
  <key>KeepAlive</key>
  <true/>
  <key>ThrottleInterval</key>
  <integer>5</integer>
  <key>EnvironmentVariables</key>
  <dict>
    <key>PATH</key>
    <string>$LAUNCH_PATH</string>
  </dict>
  <key>StandardOutPath</key>
  <string>$LOG_DIR/frontend-launchd.log</string>
  <key>StandardErrorPath</key>
  <string>$LOG_DIR/frontend-launchd.error.log</string>
</dict>
</plist>
EOF

launchctl bootout "gui/$UID_VALUE/$LABEL" 2>/dev/null || true
OLD_PID="$(lsof -nP -iTCP:$PORT -sTCP:LISTEN -t 2>/dev/null | head -1 || true)"
if [[ -n "$OLD_PID" ]]; then
  kill "$OLD_PID" 2>/dev/null || true
  sleep 1
fi
launchctl bootstrap "gui/$UID_VALUE" "$PLIST_FILE"
launchctl enable "gui/$UID_VALUE/$LABEL"
launchctl kickstart -k "gui/$UID_VALUE/$LABEL"

for _ in {1..20}; do
  if lsof -nP -iTCP:$PORT -sTCP:LISTEN >/dev/null 2>&1; then
    echo "==> 前端 LaunchAgent 已就绪：http://127.0.0.1:$PORT"
    exit 0
  fi
  sleep 1
done

echo "前端未在 $PORT 启动，请查看 $LOG_DIR/frontend-launchd.error.log" >&2
exit 1
