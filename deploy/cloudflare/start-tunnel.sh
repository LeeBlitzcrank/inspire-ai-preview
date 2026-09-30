#!/usr/bin/env bash
#
# 文件：deploy/cloudflare/start-tunnel.sh
# 所属模块：Cloudflare Worker 和隧道部署配置
# 主要职责：Shell 脚本，封装本地开发或运维命令
# 维护说明：注释解释文件边界和核心意图，具体业务规则以方法、组件和主文档说明为准。
# INSPIRE_FILE_HEADER
set -e

CONFIG_FILE="${CLOUDFLARED_CONFIG:-$HOME/.cloudflared/config.yml}"
LOG_FILE="${CLOUDFLARED_LOG:-$HOME/.cloudflared/tunnel.log}"
PID_FILE="${CLOUDFLARED_PID_FILE:-$HOME/.cloudflared/tunnel.pid}"

if ! command -v cloudflared >/dev/null 2>&1; then
  echo "==> 未安装 cloudflared，跳过隧道启动"
  exit 0
fi

if [[ ! -f "$CONFIG_FILE" ]]; then
  echo "==> 未找到 cloudflared 配置：$CONFIG_FILE"
  exit 0
fi

if pgrep -f 'cloudflared tunnel' >/dev/null 2>&1; then
  echo "==> cloudflared 隧道已在运行"
  exit 0
fi

nohup cloudflared tunnel --config "$CONFIG_FILE" run >>"$LOG_FILE" 2>&1 &
echo $! >"$PID_FILE"
echo "==> cloudflared 隧道已启动，PID=$(cat "$PID_FILE")，日志：$LOG_FILE"
