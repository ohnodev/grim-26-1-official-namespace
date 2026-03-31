#!/usr/bin/env bash
set -euo pipefail

MC_VERSION="26.1"
SERVER_DIR="tests/runs/fabric-26.1"

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
      echo "Usage: $0 [--mc-version 26.1] [--server-dir tests/runs/fabric-26.1]"
      exit 1
      ;;
  esac
done

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"
TARGET_DIR="${REPO_ROOT}/${SERVER_DIR}"
LOADER_META_URL="https://meta.fabricmc.net/v2/versions/loader/${MC_VERSION}"
INSTALLER_META_URL="https://meta.fabricmc.net/v2/versions/installer"

mkdir -p "${TARGET_DIR}" "${TARGET_DIR}/mods"

echo "[setup] Resolving Fabric loader/installer for MC ${MC_VERSION}..."
read -r LOADER_VERSION INSTALLER_VERSION < <(
  python3 - <<'PY' "${LOADER_META_URL}" "${INSTALLER_META_URL}"
import json
import sys
import time
import urllib.error
import urllib.request

loader_url = sys.argv[1]
installer_url = sys.argv[2]
MAX_ATTEMPTS = 4
TIMEOUT_SECONDS = 15

def fetch_json(url: str, label: str):
    for attempt in range(1, MAX_ATTEMPTS + 1):
        try:
            with urllib.request.urlopen(url, timeout=TIMEOUT_SECONDS) as r:
                return json.load(r)
        except (urllib.error.URLError, TimeoutError, json.JSONDecodeError) as exc:
            if attempt == MAX_ATTEMPTS:
                raise SystemExit(f"Failed fetching {label} after {MAX_ATTEMPTS} attempts: {exc}")
            time.sleep(2 ** (attempt - 1))

loader_data = fetch_json(loader_url, "loader metadata")
if not loader_data:
    raise SystemExit("No loader metadata found for requested MC version")
loader_version = loader_data[0]["loader"]["version"]

installer_data = fetch_json(installer_url, "installer metadata")
if not installer_data:
    raise SystemExit("No installer metadata found")
installer_version = installer_data[0]["version"]

print(loader_version, installer_version)
PY
)

SERVER_JAR_URL="https://meta.fabricmc.net/v2/versions/loader/${MC_VERSION}/${LOADER_VERSION}/${INSTALLER_VERSION}/server/jar"
SERVER_JAR_PATH="${TARGET_DIR}/fabric-server-launch.jar"

echo "[setup] MC=${MC_VERSION} loader=${LOADER_VERSION} installer=${INSTALLER_VERSION}"
echo "[setup] Downloading server jar..."
max_attempts=4
attempt=1
while true; do
  if curl -fsSL --connect-timeout 15 --max-time 120 "${SERVER_JAR_URL}" -o "${SERVER_JAR_PATH}"; then
    break
  fi
  if [[ "${attempt}" -ge "${max_attempts}" ]]; then
    echo "[setup] Failed to download server jar after ${max_attempts} attempts: ${SERVER_JAR_URL}" >&2
    exit 1
  fi
  sleep $((2 ** (attempt - 1)))
  attempt=$((attempt + 1))
done

cat > "${TARGET_DIR}/eula.txt" <<'EOF'
eula=true
EOF

echo "[setup] Server directory ready at: ${TARGET_DIR}"
