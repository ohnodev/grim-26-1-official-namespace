#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"
TIMEOUT_CMD=""

cd "${REPO_ROOT}"

if command -v timeout >/dev/null 2>&1; then
  TIMEOUT_CMD="timeout"
elif command -v gtimeout >/dev/null 2>&1; then
  TIMEOUT_CMD="gtimeout"
else
  echo "[all-bukkit] Missing timeout utility. Install GNU coreutils (gtimeout) or GNU timeout." >&2
  exit 1
fi

smoke_start() {
  local case_name="$1"
  shift
  local log_file
  log_file="$(mktemp)"

  set +e
  "${TIMEOUT_CMD}" 75s "$@" > >(tee "${log_file}") 2>&1
  local rc=$?
  set -e

  if [[ ${rc} -ne 0 && ${rc} -ne 124 ]]; then
    echo "[all-bukkit] ${case_name} failed with exit code ${rc}"
    rm -f "${log_file}"
    return 1
  fi

  if ! python3 - "${log_file}" <<'PY'
import pathlib
import sys
text = pathlib.Path(sys.argv[1]).read_text(encoding="utf-8", errors="ignore")
sys.exit(0 if "Done (" in text else 1)
PY
  then
    echo "[all-bukkit] ${case_name} did not reach startup completion."
    rm -f "${log_file}"
    return 1
  fi

  rm -f "${log_file}"
}

echo "[all-bukkit] Case 1/2: Paper older-version path (1.20.6)"
"${SCRIPT_DIR}/bootstrap-grim-pe-bukkit-paper-1206.sh"
smoke_start "grim-pe-paper-1206" "${SCRIPT_DIR}/run-bukkit-server.sh" --server-dir tests/runs/grim-pe-paper-1.20.6

echo "[all-bukkit] Case 2/2: Spigot path (1.21.1)"
"${SCRIPT_DIR}/bootstrap-grim-pe-bukkit-spigot-1211.sh"
smoke_start "grim-pe-spigot-1211" "${SCRIPT_DIR}/run-bukkit-server.sh" --server-dir tests/runs/grim-pe-spigot-1.21.1

echo "[all-bukkit] Grim + PacketEvents Bukkit harness complete."
