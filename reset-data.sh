#!/usr/bin/env bash
# =============================================
# 清库重启 —— 删除所有数据卷后重新初始化
# 会清除：MySQL 数据、Elasticsearch 索引、MinIO 图片、Nginx 缓存、Redis 数据
# 用途：想从零开始 / 表结构变更 / 缓存脏数据需要彻底清理时
# =============================================
set -e
cd "$(dirname "$0")"

echo "⚠️  即将执行：docker compose down -v"
echo "   这会删除以下数据卷：mysql-data / es-data / minio-data / nginx-cache"
echo "   演示数据档位：${INSPIRE_DEMO_SCALE:-small}"
read -r -p "确认清除全部数据并重启？(y/N) " ans
if [[ "$ans" != "y" && "$ans" != "Y" ]]; then
  echo "已取消。"
  exit 0
fi

# 1. 构建后端 JAR
if [[ "${SKIP_BUILD:-0}" != "1" ]]; then
  echo "==> 构建后端 JAR …"
  export JAVA_HOME="${JAVA_HOME:-/Users/lee/Library/Java/JavaVirtualMachines/jbr-21.0.8-1/Contents/Home}"
  (cd backend && mvn package \
      -pl inspire-common,inspire-mq,inspire-gateway,inspire-auth,inspire-core,inspire-admin,inspire-ai,inspire-search \
      -am -DskipTests)
fi

# 2. 删卷
echo "==> 删除数据卷 …"
docker compose down -v

# 3. 先起 MySQL 让 init.sql 建表
echo "==> 启动 MySQL 并等待初始化（约 20 秒）…"
docker compose up -d mysql
sleep 20

# 4. 起全部
echo "==> 启动全部服务 …"
# 打开演示数据开关：清库后会重新生成用户/灵感/评论/互动，以及 MinIO 里的演示图
export INSPIRE_DEMO_SEED=true
export INSPIRE_DEMO_SCALE="${INSPIRE_DEMO_SCALE:-small}"
docker compose up -d --build
sleep 5

# 5. 重建 MinIO 存储桶
echo "==> 初始化 MinIO 存储桶 …"
docker exec inspire-minio mc alias set local http://localhost:9000 minioadmin minioadmin123 || true
docker exec inspire-minio mc mb local/inspire-img || true
docker exec inspire-minio mc version enable local/inspire-img || true
bash "$(dirname "$0")/docker/minio/init-public-policy.sh"
bash "$(dirname "$0")/docker/cloudflare/start-tunnel.sh"

docker compose ps

# 前端：重新打包 +（重新）启动 dev server
if [[ "${SKIP_FRONTEND:-0}" != "1" ]]; then
  echo "==> 构建并启动前端 …"
  bash "$(dirname "$0")/frontend-up.sh"
else
  echo "==> 已跳过前端（SKIP_FRONTEND=1）"
fi

echo "==> 完成（数据库与缓存已重置）。"
