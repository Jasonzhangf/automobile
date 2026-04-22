#!/usr/bin/env bash
set -euo pipefail
repo_root="$(cd "$(dirname "$0")/../.." && pwd)"
python3 "$repo_root/scripts/dev/phase-c-extract-page-state.py" "$@"
