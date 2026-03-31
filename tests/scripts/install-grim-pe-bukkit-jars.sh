#!/usr/bin/env bash
set -euo pipefail

SERVER_DIR="tests/runs/grim-pe-paper-1.20.6"
PACKETEVENTS_REPO="/root/packetevents-grim-restructure"

while [[ $# -gt 0 ]]; do
  case "$1" in
    --server-dir)
      SERVER_DIR="${2:?missing value for --server-dir}"
      shift 2
      ;;
    --packetevents-repo)
      PACKETEVENTS_REPO="${2:?missing value for --packetevents-repo}"
      shift 2
      ;;
    *)
      echo "Unknown arg: $1"
      echo "Usage: $0 [--server-dir tests/runs/grim-pe-paper-1.20.6] [--packetevents-repo /root/packetevents-grim-restructure]"
      exit 1
      ;;
  esac
done

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"
PLUGINS_DIR="${REPO_ROOT}/${SERVER_DIR}/plugins"
GRIM_LIBS="${REPO_ROOT}/bukkit/build/libs"
PE_LIBS="${PACKETEVENTS_REPO}/build/libs"

mkdir -p "${PLUGINS_DIR}"

latest_non_doc_jar() {
  local dir="$1"
  local pattern="$2"
  local newest=""
  shopt -s nullglob
  local files=( "${dir}"/${pattern} )
  shopt -u nullglob
  local f
  for f in "${files[@]}"; do
    [[ -e "${f}" ]] || continue
    case "$(basename "${f}")" in
      *-sources.jar|*-javadoc.jar) continue ;;
    esac
    if [[ -z "${newest}" || "${f}" -nt "${newest}" ]]; then
      newest="${f}"
    fi
  done
  [[ -n "${newest}" ]] && printf '%s\n' "${newest}"
}

copy_required() {
  local file="$1"
  local label="$2"
  if [[ -z "${file}" ]]; then
    echo "[install] Missing required jar: ${label}" >&2
    exit 1
  fi
  cp -f "${file}" "${PLUGINS_DIR}/"
  echo "  - $(basename "${file}")"
}

echo "[install] Cleaning existing Grim/PacketEvents jars in ${PLUGINS_DIR}"
rm -f "${PLUGINS_DIR}"/grimac*.jar "${PLUGINS_DIR}"/packetevents*.jar

echo "[install] Copying Grim Bukkit jar:"
grim_jar="$(latest_non_doc_jar "${GRIM_LIBS}" "grimac-*.jar" || true)"
copy_required "${grim_jar}" "grimac Bukkit plugin"

echo "[install] Copying PacketEvents Spigot jar:"
pe_spigot="$(latest_non_doc_jar "${PE_LIBS}" "packetevents-spigot-*.jar" || true)"
copy_required "${pe_spigot}" "packetevents-spigot"

echo "[install] Installed plugin jars:"
ls -1 "${PLUGINS_DIR}" | sed 's/^/  - /'
