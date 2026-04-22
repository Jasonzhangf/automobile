#!/usr/bin/env bash
set -euo pipefail

purpose="${1:-tail-200}"
tail_count="${2:-200}"
device_id="${3:-}"
payload="{\"tail\": $tail_count}"
"$(cd "$(dirname "$0")" && pwd)/phase-a-send-command.sh" fetch-logs "$purpose" "$payload" "$device_id"
