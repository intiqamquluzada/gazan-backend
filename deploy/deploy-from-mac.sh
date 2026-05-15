#!/usr/bin/env bash
# Mac-də qaçır. SSH şifrəsi sənin tərəfdə interaktiv soruşulacaq.
#
# Optimized: builds Spring Boot fat-jar AND Flutter web bundle on the
# dev machine (fast: ~1 min). Ships just the artifacts. Server-side
# Docker build only does `COPY app.jar` + `COPY web/` (~10 sec).
#
# İstifadə:
#   bash deploy-from-mac.sh                 # default 209.97.135.206
#   bash deploy-from-mac.sh root@1.2.3.4    # custom server

set -euo pipefail

SERVER=${1:-root@209.97.135.206}
SERVER_IP=$(echo "$SERVER" | sed -E 's/.*@//')

# .../gazan-backend
BACKEND_DIR=$(cd "$(dirname "$0")/.." && pwd)
PARENT_DIR=$(cd "$BACKEND_DIR/.." && pwd)
MOBILE_DIR="$PARENT_DIR/gazan-mobile"

if [ ! -d "$MOBILE_DIR" ]; then
  echo "✗ gazan-mobile folder not found at $MOBILE_DIR"
  exit 1
fi

echo "──────────────────────────────────────────────────"
echo "  Server: $SERVER"
echo "  API base URL → http://$SERVER_IP"
echo "──────────────────────────────────────────────────"

# ────────────────────── 1. Backend JAR build ───────────────────────
echo
echo "▸ Building backend fat-jar (Mac) ..."
cd "$BACKEND_DIR"
if [ ! -f gradlew ]; then
  echo "  gradlew not found — running 'gradle wrapper' first ..."
  gradle wrapper --gradle-version 8.10
fi
chmod +x gradlew
./gradlew clean bootJar -x test --no-daemon
JAR=$(ls build/libs/qazan-backend-*.jar | head -1)
if [ -z "$JAR" ] || [ ! -f "$JAR" ]; then
  echo "✗ Could not find built JAR under build/libs/"
  exit 1
fi
echo "  ✓ $JAR ($(du -h "$JAR" | cut -f1))"

# ────────────────────── 2. Flutter web build ───────────────────────
echo
echo "▸ Building Flutter web (Mac) ..."
cd "$MOBILE_DIR"
if [ ! -d web ]; then
  flutter create . --platforms=web,ios,android,macos >/dev/null
fi
flutter pub get
flutter build web --release \
  --dart-define=API_BASE_URL=http://"$SERVER_IP"

# ────────────────────── 3. Pack artifact ───────────────────────────
echo
echo "▸ Packing artifact ..."
TAR=/tmp/qazan-deploy.tar.gz
rm -f "$TAR"

STAGE=$(mktemp -d)
mkdir -p "$STAGE/gazan-backend"

# Only ship what's needed at runtime: jar + web + deploy configs
cp "$BACKEND_DIR/$JAR" "$STAGE/gazan-backend/app.jar"
mkdir -p "$STAGE/gazan-backend/deploy"
cp "$BACKEND_DIR/deploy/Dockerfile.backend" "$STAGE/gazan-backend/deploy/"
cp "$BACKEND_DIR/deploy/Dockerfile.web"     "$STAGE/gazan-backend/deploy/"
cp "$BACKEND_DIR/deploy/nginx.conf"         "$STAGE/gazan-backend/deploy/"
cp "$BACKEND_DIR/deploy/docker-compose.prod.yml" "$STAGE/gazan-backend/deploy/"
cp "$BACKEND_DIR/deploy/server-setup.sh"    "$STAGE/gazan-backend/deploy/"
mkdir -p "$STAGE/gazan-backend/web"
cp -R "$MOBILE_DIR/build/web/." "$STAGE/gazan-backend/web/"

tar czf "$TAR" -C "$STAGE" gazan-backend
rm -rf "$STAGE"

SIZE=$(du -h "$TAR" | cut -f1)
echo "  → $TAR ($SIZE)"

# ────────────────────── 4. Upload + run on server ──────────────────
echo
echo "▸ Uploading to server (will prompt for SSH password) ..."
scp "$TAR" "$SERVER":/root/qazan-deploy.tar.gz

echo
echo "▸ Running setup on server (will prompt for SSH password) ..."
ssh "$SERVER" 'bash -s' <<'REMOTE'
set -euo pipefail
rm -rf /opt/qazan
mkdir -p /opt/qazan
tar xzf /root/qazan-deploy.tar.gz -C /opt/qazan
cd /opt/qazan/gazan-backend
chmod +x deploy/server-setup.sh
bash deploy/server-setup.sh
REMOTE

echo
echo "──────────────────────────────────────────────────"
echo "✓ Tamam! Aç:  http://$SERVER_IP"
echo "──────────────────────────────────────────────────"
