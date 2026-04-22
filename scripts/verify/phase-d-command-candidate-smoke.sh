#!/usr/bin/env bash
set -euo pipefail

repo_root="$(cd "$(dirname "$0")/../.." && pwd)"
page_state="$repo_root/.tmp/page-state/phase-c-smoke.json"
out="$repo_root/.tmp/page-state/phase-d-smoke.json"

mkdir -p "$(dirname "$out")"
"$repo_root/scripts/dev/phase-d-extract-command-candidates.sh" \
  --page-state "$page_state" \
  --output "$out" >/dev/null

python3 - <<'PY' "$out"
import json
import sys

path = sys.argv[1]
with open(path) as f:
    data = json.load(f)

assert data["schemaVersion"] == "exp01-command-candidates-v1"
assert data["page"]["title"] == "无线调试"
assert any(item["commandId"] == "tap:view:coui_toolbar_back_view" for item in data["commands"])
assert any(item["commandId"] == "scroll-forward:view:recycler_view" for item in data["commands"])
assert any(item["commandId"] == "tap:role:action|label:使用二维码配对设备" for item in data["commands"])
assert data["stats"]["commandCount"] >= 8
print(path)
PY
