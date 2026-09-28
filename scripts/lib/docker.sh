#!/usr/bin/env bash

ensure_docker_running() {
  if docker info >/dev/null 2>&1; then
    echo "==> Docker 已就绪"
    return 0
  fi

  echo "==> Docker daemon 未运行，正在尝试启动 Docker Desktop …"
  case "$(uname -s)" in
    Darwin)
      if [[ -d "/Applications/Docker.app" || -d "$HOME/Applications/Docker.app" ]]; then
        open -a Docker >/dev/null 2>&1 || true
      elif command -v colima >/dev/null 2>&1; then
        colima start >/dev/null 2>&1 || true
      fi
      ;;
    Linux)
      if command -v systemctl >/dev/null 2>&1; then
        sudo systemctl start docker >/dev/null 2>&1 || true
      fi
      ;;
  esac

  local timeout="${DOCKER_START_TIMEOUT:-120}"
  case "$timeout" in
    ''|*[!0-9]*) timeout=120 ;;
  esac

  local waited=0
  while (( waited < timeout )); do
    sleep 2
    waited=$((waited + 2))
    if docker info >/dev/null 2>&1; then
      echo "==> Docker 已启动（等待 ${waited}s）"
      return 0
    fi
  done

  echo "错误：Docker 未能在 ${timeout}s 内启动，请手动打开 Docker Desktop 后重试。"
  return 1
}
