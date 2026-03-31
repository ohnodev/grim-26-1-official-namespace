#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"
PACKETEVENTS_REPO="${PACKETEVENTS_REPO:-/root/packetevents-grim-restructure}"

cd "${REPO_ROOT}"

echo "[bootstrap:bukkit-paper-1206] Building Grim Bukkit artifact..."
./gradlew -PmavenLocalOverride=true :bukkit:shadowJar

echo "[bootstrap:bukkit-paper-1206] Building PacketEvents Spigot artifact..."
./gradlew -p "${PACKETEVENTS_REPO}" :spigot:build

echo "[bootstrap:bukkit-paper-1206] Setting up Paper server (1.20.6)..."
"${SCRIPT_DIR}/setup-paper-server.sh" \
  --mc-version 1.20.6 \
  --server-dir tests/runs/grim-pe-paper-1.20.6

echo "[bootstrap:bukkit-paper-1206] Installing Grim + PacketEvents plugins..."
"${SCRIPT_DIR}/install-grim-pe-bukkit-jars.sh" \
  --server-dir tests/runs/grim-pe-paper-1.20.6 \
  --packetevents-repo "${PACKETEVENTS_REPO}"

echo
echo "[bootstrap:bukkit-paper-1206] Ready."
