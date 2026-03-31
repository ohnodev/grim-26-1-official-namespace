#!/usr/bin/env bash
set -euo pipefail

SERVER_DIR="tests/runs/fabric-26.1"
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
      echo "Usage: $0 [--server-dir tests/runs/fabric-26.1] [--java-bin /path/to/java]"
      exit 1
      ;;
  esac
done

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"
TARGET_DIR="${REPO_ROOT}/${SERVER_DIR}"
SERVER_JAR="${TARGET_DIR}/fabric-server-launch.jar"

if [[ ! -f "${SERVER_JAR}" ]]; then
  echo "Missing ${SERVER_JAR}" >&2
  echo "Run a bootstrap script first." >&2
  exit 1
fi

cd "${TARGET_DIR}"
exec "${JAVA_BIN}" -Xms1G -Xmx2G -jar fabric-server-launch.jar nogui
