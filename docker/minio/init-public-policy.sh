#!/usr/bin/env bash
set -e

ROOT_DIR="$(cd "$(dirname "$0")/../.." && pwd)"
POLICY_FILE="$ROOT_DIR/docker/minio/inspire-img-policy.json"

docker cp "$POLICY_FILE" inspire-minio:/tmp/inspire-img-policy.json
docker exec inspire-minio sh -c 'mc alias set local http://localhost:9000 "$MINIO_ROOT_USER" "$MINIO_ROOT_PASSWORD"' >/dev/null
docker exec inspire-minio mc anonymous set-json /tmp/inspire-img-policy.json local/inspire-img
