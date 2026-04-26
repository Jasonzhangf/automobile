#!/usr/bin/env bash
set -euo pipefail

if [ "$#" -lt 1 ]; then
  echo "usage: $0 <payload-json-path> [purpose] [device-id]" >&2
  exit 1
fi

repo_root="$(cd "$(dirname "$0")/../.." && pwd)"
payload_path="$1"
purpose="${2:-workflow-step}"
device_id="${3:-}"

if [ ! -f "$payload_path" ]; then
  echo "missing payload file: $payload_path" >&2
  exit 1
fi

payload_json="$(python3 - <<'PY' "$payload_path"
import json, sys
print(json.dumps(json.load(open(sys.argv[1])), ensure_ascii=False, separators=(",", ":")))
PY
)"

"$repo_root/scripts/dev/phase-a-send-command.sh" run-workflow-step "$purpose" "$payload_json" "$device_id"
