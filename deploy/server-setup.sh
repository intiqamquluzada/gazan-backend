#!/usr/bin/env bash
# Runs on the Ubuntu server. Idempotent — safe to re-run.
# Expects to be executed from /opt/qazan/gazan-backend/.

set -euo pipefail

cd "$(dirname "$0")/.."

# ────────────────────── 1. Install Docker ──────────────────────
if ! command -v docker >/dev/null 2>&1; then
  echo "▸ Docker is missing — installing ..."
  export DEBIAN_FRONTEND=noninteractive
  apt-get update -qq
  apt-get install -y -qq ca-certificates curl gnupg lsb-release
  install -m 0755 -d /etc/apt/keyrings
  curl -fsSL https://download.docker.com/linux/ubuntu/gpg \
    | gpg --dearmor -o /etc/apt/keyrings/docker.gpg
  chmod a+r /etc/apt/keyrings/docker.gpg
  echo \
    "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] \
     https://download.docker.com/linux/ubuntu $(. /etc/os-release && echo "$VERSION_CODENAME") stable" \
    > /etc/apt/sources.list.d/docker.list
  apt-get update -qq
  apt-get install -y -qq docker-ce docker-ce-cli containerd.io \
    docker-buildx-plugin docker-compose-plugin
fi

# ────────────────────── 2. Firewall ────────────────────────────
if command -v ufw >/dev/null 2>&1; then
  ufw allow OpenSSH >/dev/null 2>&1 || true
  ufw allow 80/tcp  >/dev/null 2>&1 || true
  ufw allow 443/tcp >/dev/null 2>&1 || true
  yes | ufw enable  >/dev/null 2>&1 || true
fi

# ────────────────────── 3. Secrets (.env) ──────────────────────
ENV_FILE="deploy/.env"
if [ ! -f "$ENV_FILE" ]; then
  echo "▸ Generating secrets ..."
  POSTGRES_PASSWORD=$(head -c 24 /dev/urandom | base64 | tr -d '/+=')
  JWT_SECRET=$(head -c 48 /dev/urandom | base64 | tr -d '/+=')
  cat > "$ENV_FILE" <<EOF
POSTGRES_PASSWORD=$POSTGRES_PASSWORD
JWT_SECRET=$JWT_SECRET
EOF
  chmod 600 "$ENV_FILE"
fi

# ────────────────────── 4. Build + run ─────────────────────────
echo "▸ Building containers (this can take 5–10 min the first time) ..."
docker compose --env-file deploy/.env -f deploy/docker-compose.prod.yml \
  up -d --build

echo "▸ Waiting for backend health ..."
for i in $(seq 1 60); do
  if curl -fsS http://localhost/actuator/health >/dev/null 2>&1; then
    echo "✓ Backend is UP"
    break
  fi
  sleep 2
done

echo
echo "─────────────────────────────────────────────────"
echo "✓ Deployment finished."
echo "  Web app:  http://$(curl -s ifconfig.me)"
echo "  API docs: http://$(curl -s ifconfig.me)/docs"
echo "─────────────────────────────────────────────────"
docker compose --env-file deploy/.env -f deploy/docker-compose.prod.yml ps
