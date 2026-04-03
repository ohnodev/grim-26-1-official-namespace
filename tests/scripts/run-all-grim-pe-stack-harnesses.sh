#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

echo "[all-stack] Running Fabric 26.1 harness..."
"${SCRIPT_DIR}/run-all-grim-pe-harnesses.sh"
echo "[all-stack] Fabric-only stack harness complete."
