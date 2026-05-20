#!/usr/bin/env bash
# Yalnız backend-i yenidən deploy edir — Flutter web toxunulmur.
# App artıq serverdə var, sadəcə backend JAR-ı yenilənir.
#
# İstifadə:
#   bash deploy/deploy-backend-only.sh                 # default 209.97.135.206
#   bash deploy/deploy-backend-only.sh root@1.2.3.4    # custom server

set -euo pipefail

SERVER=${1:-root@209.97.135.206}
BACKEND_DIR=$(cd "$(dirname "$0")/.." && pwd)

echo "──────────────────────────────────────────────────"
echo "  Backend-only deploy → $SERVER"
echo "──────────────────────────────────────────────────"

# ────────────────────── 1. Build JAR (Mac) ─────────────────────────
echo
echo "▸ Building backend fat-jar ..."
cd "$BACKEND_DIR"
if [ ! -f gradlew ]; then
  gradle wrapper --gradle-version 8.10
fi
chmod +x gradlew
./gradlew clean bootJar -x test --no-daemon
JAR=$(ls build/libs/qazan-backend-*.jar | head -1)
if [ -z "$JAR" ] || [ ! -f "$JAR" ]; then
  echo "✗ JAR not found under build/libs/"
  exit 1
fi
echo "  ✓ $JAR ($(du -h "$JAR" | cut -f1))"

# ────────────────────── 2. Pack (jar + backend Dockerfile) ─────────
echo
echo "▸ Packing ..."
TAR=/tmp/qazan-backend-only.tar.gz
rm -f "$TAR"
STAGE=$(mktemp -d)
mkdir -p "$STAGE/payload/deploy"
cp "$JAR" "$STAGE/payload/app.jar"
cp deploy/Dockerfile.backend "$STAGE/payload/deploy/Dockerfile.backend"
tar czf "$TAR" -C "$STAGE" payload
rm -rf "$STAGE"
echo "  → $TAR ($(du -h "$TAR" | cut -f1))"

# ────────────────────── 3. Upload + rebuild backend only ───────────
echo
echo "▸ Uploading (SSH password gözlənilir) ..."
scp "$TAR" "$SERVER":/root/qazan-backend-only.tar.gz

echo
echo "▸ Rebuilding backend container on server ..."
ssh "$SERVER" 'bash -s' <<'REMOTE'
set -euo pipefail
APP=/opt/qazan/gazan-backend
if [ ! -d "$APP" ]; then
  echo "✗ $APP not found — run a full deploy first (deploy-from-mac.sh)"
  exit 1
fi

# Extract just the new jar + backend Dockerfile, leave web/ and configs alone
tar xzf /root/qazan-backend-only.tar.gz -C /tmp/
cp /tmp/payload/app.jar "$APP/app.jar"
cp /tmp/payload/deploy/Dockerfile.backend "$APP/deploy/Dockerfile.backend"
rm -rf /tmp/payload

cd "$APP"
# --build backend → only the backend service is rebuilt; postgres + web stay up
docker compose -f deploy/docker-compose.prod.yml --env-file deploy/.env \
  up -d --build backend

echo "▸ Backend health (10s) ..."
sleep 12
for i in $(seq 1 30); do
  if curl -fsS http://localhost/actuator/health >/dev/null 2>&1; then
    echo "✓ Backend UP"
    break
  fi
  sleep 2
done
docker compose -f deploy/docker-compose.prod.yml --env-file deploy/.env ps
REMOTE

echo
echo "──────────────────────────────────────────────────"
echo "✓ Backend deployed. Flutter web toxunulmadı."
echo "  API:  http://${SERVER#*@}/api/v1"
echo "  Docs: http://${SERVER#*@}/docs"
echo "──────────────────────────────────────────────────"
