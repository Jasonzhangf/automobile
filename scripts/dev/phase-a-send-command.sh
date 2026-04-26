#!/usr/bin/env bash
set -euo pipefail

if [ "$#" -lt 1 ]; then
  echo "usage: $0 <command> [purpose] [payload-json] [device-id] [timeout-ms]" >&2
  exit 1
fi

command_name="$1"
purpose="${2:-manual-dispatch}"
payload_json="${3-}"
if [ -z "$payload_json" ]; then
  payload_json='{}'
fi
explicit_device_id="${4:-}"
timeout_ms="${5:-30000}"
base_url="${FLOWY_MAC_DAEMON_BASE_URL:-http://127.0.0.1:8787}"

clients_json="$(curl -sSf "$base_url/exp01/clients")"

device_id="$explicit_device_id"
if [ -z "$device_id" ]; then
  device_id="$(CLIENTS_JSON="$clients_json" python3 - <<'PY'
import json, os, sys
clients = json.loads(os.environ['CLIENTS_JSON'])
if not clients:
    sys.exit(2)
if len(clients) > 1:
    sys.exit(3)
print(clients[0]['deviceId'])
PY
)" || status=$?;
  status="${status:-0}"
  if [ "$status" -eq 2 ]; then
    echo "no connected exp01 client" >&2
    exit 1
  fi
  if [ "$status" -eq 3 ]; then
    echo "multiple exp01 clients connected; pass device-id explicitly" >&2
    echo "$clients_json" | python3 -m json.tool >&2
    exit 1
  fi
fi

body="$(COMMAND_NAME="$command_name" PURPOSE="$purpose" PAYLOAD_JSON="$payload_json" DEVICE_ID="$device_id" TIMEOUT_MS="$timeout_ms" python3 - <<'PY'
import json
import os
from datetime import datetime

command_name = os.environ["COMMAND_NAME"]
purpose = os.environ["PURPOSE"]
payload = json.loads(os.environ["PAYLOAD_JSON"])
timeout_ms = int(os.environ.get("TIMEOUT_MS", "30000"))
run_id = datetime.now().strftime('%Y-%m-%dT%H-%M-%S') + '_' + command_name.replace('_','-') + '_' + purpose.replace('_','-')

print(json.dumps({
  'deviceId': os.environ["DEVICE_ID"],
  'command': {
    'command': command_name,
    'payload': payload,
    'runId': run_id,
    'timeoutMs': timeout_ms,
  }
}))
PY
)"

curl -sSf -X POST "$base_url/exp01/command" \
  -H 'Content-Type: application/json' \
  -d "$body" | python3 -m json.tool
