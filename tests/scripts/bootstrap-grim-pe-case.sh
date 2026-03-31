#!/usr/bin/env bash
set -euo pipefail

CASE_NAME=""
MC_VERSION=""
SERVER_DIR=""
PROFILE=""
PACKETEVENTS_REPO="/root/packetevents-grim-restructure"
JAVA25_HOME="${JAVA25_HOME:-/root/.gradle/jdks/eclipse_adoptium-25-amd64-linux.2}"

while [[ $# -gt 0 ]]; do
  case "$1" in
    --case)
      CASE_NAME="${2:?missing value for --case}"
      shift 2
      ;;
    --mc-version)
      MC_VERSION="${2:?missing value for --mc-version}"
      shift 2
      ;;
    --server-dir)
      SERVER_DIR="${2:?missing value for --server-dir}"
      shift 2
      ;;
    --profile)
      PROFILE="${2:?missing value for --profile}"
      shift 2
      ;;
    --packetevents-repo)
      PACKETEVENTS_REPO="${2:?missing value for --packetevents-repo}"
      shift 2
      ;;
    --java25-home)
      JAVA25_HOME="${2:?missing value for --java25-home}"
      shift 2
      ;;
    *)
      echo "Unknown arg: $1"
      echo "Usage: $0 --case name --mc-version 26.1 --server-dir tests/runs/name --profile official-261|intermediary-1216 [--packetevents-repo /root/packetevents-grim-restructure]"
      exit 1
      ;;
  esac
done

if [[ -z "${CASE_NAME}" || -z "${MC_VERSION}" || -z "${SERVER_DIR}" || -z "${PROFILE}" ]]; then
  echo "Missing required args."
  exit 1
fi

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"

cd "${REPO_ROOT}"

echo "[bootstrap:${CASE_NAME}] Building Grim fabric artifact (local override enabled)..."
./gradlew -PmavenLocalOverride=true :fabric:jar

echo "[bootstrap:${CASE_NAME}] Building PacketEvents fabric artifacts..."
case "${PROFILE}" in
  official-261)
    ./gradlew -p "${PACKETEVENTS_REPO}" -Dorg.gradle.java.home="${JAVA25_HOME}" clean :fabric:build :fabric-common:build :fabric-official:build
    ;;
  intermediary-1216)
    ./gradlew -p "${PACKETEVENTS_REPO}" clean :fabric:build :fabric-intermediary:build
    ;;
  *)
    echo "[bootstrap:${CASE_NAME}] Unknown profile: ${PROFILE}" >&2
    exit 1
    ;;
esac

echo "[bootstrap:${CASE_NAME}] Setting up Fabric server ${MC_VERSION}..."
"${SCRIPT_DIR}/setup-fabric-server.sh" \
  --mc-version "${MC_VERSION}" \
  --server-dir "${SERVER_DIR}"

echo "[bootstrap:${CASE_NAME}] Installing Grim + PacketEvents jars (${PROFILE})..."
"${SCRIPT_DIR}/install-grim-pe-jars.sh" \
  --server-dir "${SERVER_DIR}" \
  --packetevents-repo "${PACKETEVENTS_REPO}" \
  --profile "${PROFILE}"

echo
echo "[bootstrap:${CASE_NAME}] Ready."
