#!/usr/bin/env bash
set -euo pipefail

MC_VERSION="1.21.1"
SERVER_DIR="tests/runs/grim-pe-spigot-1.21.1"
JAVA_BIN="java"
REBUILD="0"

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
    --java-bin)
      JAVA_BIN="${2:?missing value for --java-bin}"
      shift 2
      ;;
    --rebuild)
      REBUILD="1"
      shift
      ;;
    *)
      echo "Unknown arg: $1"
      echo "Usage: $0 [--mc-version 1.21.1] [--server-dir tests/runs/grim-pe-spigot-1.21.1] [--java-bin /path/to/java] [--rebuild]"
      exit 1
      ;;
  esac
done

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"
TARGET_DIR="${REPO_ROOT}/${SERVER_DIR}"
CACHE_DIR="${REPO_ROOT}/tests/.cache/buildtools"
BUILDTOOLS_JAR="${CACHE_DIR}/BuildTools.jar"

mkdir -p "${TARGET_DIR}" "${TARGET_DIR}/plugins" "${CACHE_DIR}"

echo "[setup] Downloading BuildTools..."
curl -fsSL "https://hub.spigotmc.org/jenkins/job/BuildTools/lastSuccessfulBuild/artifact/target/BuildTools.jar" -o "${BUILDTOOLS_JAR}"

SPIGOT_JAR="$(ls -t "${CACHE_DIR}"/spigot-"${MC_VERSION}"*.jar 2>/dev/null | head -n 1 || true)"
if [[ "${REBUILD}" == "0" && -n "${SPIGOT_JAR}" ]]; then
  echo "[setup] Reusing cached Spigot jar: $(basename "${SPIGOT_JAR}")"
else
  echo "[setup] Building Spigot server for MC ${MC_VERSION} (this can take several minutes)..."
  (
    cd "${CACHE_DIR}"
    "${JAVA_BIN}" -jar "${BUILDTOOLS_JAR}" --rev "${MC_VERSION}" --compile SPIGOT --output-dir "${CACHE_DIR}"
  )
  SPIGOT_JAR="$(ls -t "${CACHE_DIR}"/spigot-"${MC_VERSION}"*.jar 2>/dev/null | head -n 1 || true)"
fi

if [[ -z "${SPIGOT_JAR}" ]]; then
  echo "[setup] Could not locate built Spigot jar in ${CACHE_DIR}" >&2
  exit 1
fi

cp -f "${SPIGOT_JAR}" "${TARGET_DIR}/spigot-server.jar"

cat > "${TARGET_DIR}/eula.txt" <<'EOF'
eula=true
EOF

echo "[setup] Server directory ready at: ${TARGET_DIR}"
