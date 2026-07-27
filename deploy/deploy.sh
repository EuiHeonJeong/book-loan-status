#!/usr/bin/env bash
set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

cd "$REPO_ROOT"
echo "==> git pull"
git pull --ff-only

echo "==> docker compose build + up"
docker compose -f "$SCRIPT_DIR/docker-compose.yml" --env-file "$SCRIPT_DIR/.env" up -d --build

echo "==> pruning unused images (Always Free 부팅 볼륨은 대략 50GB)"
docker image prune -f

echo "==> status"
docker compose -f "$SCRIPT_DIR/docker-compose.yml" ps
