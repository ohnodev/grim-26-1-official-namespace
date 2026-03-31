#!/usr/bin/env bash
set -euo pipefail

MC_VERSION="1.20.6"
SERVER_DIR="tests/runs/grim-pe-paper-1.20.6"

while [[ $# -gt 0 ]]; do
  case "$1" in
    --mc-version)
      MC_VERSION="${2:?missing value for --mc-version}"
      shift 2
      ;;
    --server-dir)
      SERVER_DIR="${2:?missing value for --server-dir}"
      shift 2
      ;;
    *)
      echo "Unknown arg: $1"
      echo "Usage: $0 [--mc-version 1.20.6] [--server-dir tests/runs/grim-pe-paper-1.20.6]"
      exit 1
      ;;
  esac
done

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"
TARGET_DIR="${REPO_ROOT}/${SERVER_DIR}"
PAPER_API_BASE="https://api.papermc.io/v2/projects/paper/versions/${MC_VERSION}"

mkdir -p "${TARGET_DIR}" "${TARGET_DIR}/plugins"

echo "[setup] Resolving latest Paper build for MC ${MC_VERSION}..."
read -r BUILD_NUMBER DOWNLOAD_NAME < <(
  python3 - <<'PY' "${PAPER_API_BASE}/builds"
import json
import sys
import urllib.request

url = sys.argv[1]
with urllib.request.urlopen(url, timeout=20) as r:
    builds = json.load(r).get("builds", [])
if not builds:
    raise SystemExit("No Paper builds found for requested version")
latest = builds[-1]
download_name = latest["downloads"]["application"]["name"]
print(latest["build"], download_name)
PY
)

SERVER_JAR_URL="${PAPER_API_BASE}/builds/${BUILD_NUMBER}/downloads/${DOWNLOAD_NAME}"
SERVER_JAR_PATH="${TARGET_DIR}/paper-server.jar"

echo "[setup] MC=${MC_VERSION} paperBuild=${BUILD_NUMBER}"
echo "[setup] Downloading Paper server jar..."
curl -fsSL "${SERVER_JAR_URL}" -o "${SERVER_JAR_PATH}"

cat > "${TARGET_DIR}/eula.txt" <<'EOF'
eula=true
EOF

echo "[setup] Server directory ready at: ${TARGET_DIR}"
