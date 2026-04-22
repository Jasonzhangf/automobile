#!/usr/bin/env bash
set -euo pipefail

purpose="${1:-boot-check}"
device_id="${2:-}"
"$(cd "$(dirname "$0")" && pwd)/phase-a-send-command.sh" ping "$purpose" '{}' "$device_id"
