#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

"${SCRIPT_DIR}/bootstrap-grim-pe-case.sh" \
  --case grim-pe-intermediary-1216 \
  --mc-version 1.21.6 \
  --server-dir tests/runs/grim-pe-1.21.6 \
  --profile intermediary-1216 \
  "$@"
