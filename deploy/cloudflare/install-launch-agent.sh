#!/usr/bin/env bash
#
# 文件：deploy/cloudflare/install-launch-agent.sh
# 所属模块：Cloudflare Worker 和隧道部署配置
# 主要职责：将 cloudflared 注册为 macOS 用户级常驻服务
# 维护说明：只修改当前用户的 LaunchAgents，不需要 root 权限。
# INSPIRE_FILE_HEADER
set -euo pipefail

UID_VALUE="$(id -u)"
LABEL="com.inspire.cloudflared"
CONFIG_FILE="${CLOUDFLARED_CONFIG:-$HOME/.cloudflared/config.yml}"
LOG_DIR="$HOME/.cloudflared"
PLIST_DIR="$HOME/Library/LaunchAgents"
PLIST_FILE="$PLIST_DIR/$LABEL.plist"
CLOUDFLARED_BIN="$(command -v cloudflared || true)"

if [[ -z "$CLOUDFLARED_BIN" ]]; then
  echo "错误：未安装 cloudflared。" >&2
  exit 1
fi

if [[ ! -f "$CONFIG_FILE" ]]; then
  echo "错误：未找到 cloudflared 配置：$CONFIG_FILE" >&2
  exit 1
fi

mkdir -p "$LOG_DIR" "$PLIST_DIR"

cat >"$PLIST_FILE" <<EOF
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
<dict>
  <key>Label</key>
  <string>$LABEL</string>
  <key>ProgramArguments</key>
  <array>
    <string>$CLOUDFLARED_BIN</string>
    <string>tunnel</string>
    <string>--config</string>
    <string>$CONFIG_FILE</string>
    <string>run</string>
  </array>
  <key>RunAtLoad</key>
  <true/>
  <key>KeepAlive</key>
  <true/>
  <key>ThrottleInterval</key>
  <integer>10</integer>
  <key>ProcessType</key>
  <string>Background</string>
  <key>StandardOutPath</key>
  <string>$LOG_DIR/tunnel-launchd.log</string>
  <key>StandardErrorPath</key>
  <string>$LOG_DIR/tunnel-launchd.error.log</string>
</dict>
</plist>
EOF

launchctl bootout "gui/$UID_VALUE/$LABEL" 2>/dev/null || true
pkill -f "cloudflared tunnel --config $CONFIG_FILE run" 2>/dev/null || true
sleep 1
launchctl bootstrap "gui/$UID_VALUE" "$PLIST_FILE"
launchctl enable "gui/$UID_VALUE/$LABEL"
launchctl kickstart -k "gui/$UID_VALUE/$LABEL"

echo "==> cloudflared LaunchAgent 已安装：$PLIST_FILE"
launchctl print "gui/$UID_VALUE/$LABEL" | sed -n '1,40p'
