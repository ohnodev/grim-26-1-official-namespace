#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"
JAVA25_BIN="${JAVA25_BIN:-/root/.gradle/jdks/eclipse_adoptium-25-amd64-linux.2/bin/java}"
TIMEOUT_CMD=""

cd "${REPO_ROOT}"

if command -v timeout >/dev/null 2>&1; then
  TIMEOUT_CMD="timeout"
elif command -v gtimeout >/dev/null 2>&1; then
  TIMEOUT_CMD="gtimeout"
else
  echo "[all] Missing timeout utility. Install GNU coreutils (gtimeout) or GNU timeout." >&2
  exit 1
fi

smoke_start() {
  local case_name="$1"
  shift
  local log_file
  log_file="$(mktemp)"

  set +e
  "${TIMEOUT_CMD}" 60s "$@" > >(tee "${log_file}") 2>&1
  local rc=$?
  set -e

  if [[ ${rc} -ne 0 && ${rc} -ne 124 ]]; then
    echo "[all] ${case_name} failed with exit code ${rc}"
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
    echo "[all] ${case_name} did not reach startup completion."
    rm -f "${log_file}"
    return 1
  fi

  rm -f "${log_file}"
}

echo "[all] Case 1/1: Grim + PE official (26.1)"
if [[ ! -x "${JAVA25_BIN}" ]]; then
  echo "[all] Missing Java 25 binary at ${JAVA25_BIN}"
  echo "[all] Set JAVA25_BIN to your Java 25 executable and retry."
  exit 1
fi
"${SCRIPT_DIR}/bootstrap-grim-pe-official-261.sh"
smoke_start "grim-pe-official-261" "${SCRIPT_DIR}/run-server.sh" --server-dir tests/runs/grim-pe-26.1 --java-bin "${JAVA25_BIN}"

echo "[all] Grim + PacketEvents harness complete."
