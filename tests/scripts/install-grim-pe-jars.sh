#!/usr/bin/env bash
set -euo pipefail

SERVER_DIR="tests/runs/fabric-26.1"
PACKETEVENTS_REPO="/root/packetevents-grim-restructure"
PROFILE="official-261"
FABRIC_API_VERSION="0.144.4+26.1"

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
    --profile)
      PROFILE="${2:?missing value for --profile}"
      shift 2
      ;;
    --fabric-api-version)
      FABRIC_API_VERSION="${2:?missing value for --fabric-api-version}"
      shift 2
      ;;
    *)
      echo "Unknown arg: $1"
      echo "Usage: $0 [--server-dir tests/runs/fabric-26.1] [--packetevents-repo /root/packetevents-grim-restructure] [--profile official-261|intermediary-1216] [--fabric-api-version 0.144.4+26.1]"
      exit 1
      ;;
  esac
done

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"
MODS_DIR="${REPO_ROOT}/${SERVER_DIR}/mods"
GRIM_LIBS="${REPO_ROOT}/fabric/build/libs"
PE_LIBS="${PACKETEVENTS_REPO}/build/libs"

mkdir -p "${MODS_DIR}"

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
  cp -f "${file}" "${MODS_DIR}/"
  echo "  - $(basename "${file}")"
}

copy_pe_module() {
  local module="$1"
  local f
  f="$(latest_non_doc_jar "${PE_LIBS}" "packetevents-fabric-${module}-*.jar" || true)"
  copy_required "${f}" "packetevents-fabric-${module}"
}

install_fabric_api() {
  local file="${MODS_DIR}/fabric-api-${FABRIC_API_VERSION}.jar"
  if [[ -f "${file}" ]]; then
    echo "  - $(basename "${file}")"
    return
  fi
  local url="https://maven.fabricmc.net/net/fabricmc/fabric-api/fabric-api/${FABRIC_API_VERSION}/fabric-api-${FABRIC_API_VERSION}.jar"
  echo "[install] Downloading fabric-api ${FABRIC_API_VERSION}..."
  curl -fsSL --connect-timeout 15 --max-time 120 "${url}" -o "${file}"
  echo "  - $(basename "${file}")"
}

echo "[install] Cleaning existing Grim/PacketEvents jars in ${MODS_DIR}"
rm -f "${MODS_DIR}"/grimac-fabric*.jar "${MODS_DIR}"/packetevents*.jar

echo "[install] Copying Grim jar:"
grim_jar="$(latest_non_doc_jar "${GRIM_LIBS}" "grimac-fabric-*.jar" || true)"
copy_required "${grim_jar}" "grimac-fabric"

echo "[install] Copying PacketEvents jars for profile ${PROFILE}:"

pe_common="$(latest_non_doc_jar "${PE_LIBS}" "packetevents-fabric-common-*.jar" || true)"
copy_required "${pe_common}" "packetevents-fabric-common"

case "${PROFILE}" in
  official-261)
    pe_main="$(latest_non_doc_jar "${PE_LIBS}" "packetevents-fabric-*.jar" | awk '!/fabric-common|fabric-intermediary|fabric-official|fabric-mc/' | head -n 1 || true)"
    if [[ -z "${pe_main}" ]]; then
      echo "[install] Missing required jar: packetevents-fabric main" >&2
      exit 1
    fi
    copy_required "${pe_main}" "packetevents-fabric main"
    pe_official="$(latest_non_doc_jar "${PE_LIBS}" "packetevents-fabric-official-*.jar" || true)"
    copy_required "${pe_official}" "packetevents-fabric-official"
    ;;
  intermediary-1216)
    pe_main="$(latest_non_doc_jar "${PE_LIBS}" "packetevents-fabric-*.jar" | awk '!/fabric-common|fabric-intermediary|fabric-official|fabric-mc/' | head -n 1 || true)"
    if [[ -z "${pe_main}" ]]; then
      echo "[install] Missing required jar: packetevents-fabric main" >&2
      exit 1
    fi
    copy_required "${pe_main}" "packetevents-fabric main"
    pe_intermediary="$(latest_non_doc_jar "${PE_LIBS}" "packetevents-fabric-intermediary-*.jar" || true)"
    copy_required "${pe_intermediary}" "packetevents-fabric-intermediary"
    copy_pe_module "mc1140"
    copy_pe_module "mc1194"
    copy_pe_module "mc1202"
    copy_pe_module "mc1211"
    copy_pe_module "mc1215"
    copy_pe_module "mc1216"
    ;;
  *)
    echo "[install] Unknown profile: ${PROFILE}" >&2
    echo "[install] Expected official-261 or intermediary-1216" >&2
    exit 1
    ;;
esac

echo "[install] Ensuring Fabric API runtime dependency:"
install_fabric_api

echo "[install] Installed jars:"
ls -1 "${MODS_DIR}" | sed 's/^/  - /'
