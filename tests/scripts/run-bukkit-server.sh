#!/usr/bin/env bash
set -euo pipefail

SERVER_DIR="tests/runs/grim-pe-paper-1.20.6"
JAVA_BIN="java"

while [[ $# -gt 0 ]]; do
  case "$1" in
    --server-dir)
      SERVER_DIR="${2:?missing value for --server-dir}"
      shift 2
      ;;
    --java-bin)
      JAVA_BIN="${2:?missing value for --java-bin}"
      shift 2
      ;;
    *)
      echo "Unknown arg: $1"
      echo "Usage: $0 [--server-dir tests/runs/grim-pe-paper-1.20.6] [--java-bin /path/to/java]"
      exit 1
      ;;
  esac
done

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"
TARGET_DIR="${REPO_ROOT}/${SERVER_DIR}"

SERVER_JAR=""
if [[ -f "${TARGET_DIR}/paper-server.jar" ]]; then
  SERVER_JAR="${TARGET_DIR}/paper-server.jar"
elif [[ -f "${TARGET_DIR}/spigot-server.jar" ]]; then
  SERVER_JAR="${TARGET_DIR}/spigot-server.jar"
fi

if [[ -z "${SERVER_JAR}" ]]; then
  echo "Missing server jar in ${TARGET_DIR}" >&2
  echo "Run setup first (paper or spigot bootstrap)." >&2
  exit 1
fi

cd "${TARGET_DIR}"
exec "${JAVA_BIN}" -Xms1G -Xmx2G -jar "${SERVER_JAR}" nogui
