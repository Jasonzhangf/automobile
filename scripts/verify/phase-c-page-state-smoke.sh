#!/usr/bin/env bash
set -euo pipefail

repo_root="$(cd "$(dirname "$0")/../.." && pwd)"
raw="$repo_root/artifacts/2026-04-22/2026-04-22T12-36-31_dump-accessibility-tree_settings-tree/accessibility-raw.json"
context="$repo_root/artifacts/2026-04-22/2026-04-22T12-36-31_dump-accessibility-tree_settings-tree/page-context.json"
out="$repo_root/.tmp/page-state/phase-c-smoke.json"

mkdir -p "$(dirname "$out")"
"$repo_root/scripts/dev/phase-c-extract-page-state.sh" \
  --accessibility-raw "$raw" \
  --page-context "$context" \
  --output "$out" >/dev/null

python3 - <<'PY' "$out"
import json
import sys

path = sys.argv[1]
with open(path) as f:
    data = json.load(f)

assert data["schemaVersion"] == "exp01-page-state-v1"
assert data["app"]["packageName"] == "com.android.settings"
assert data["page"]["title"] == "无线调试"
assert data["page"]["stats"]["elementCount"] >= 5
assert any(item["label"] == "返回" and "click" in item["actions"] for item in data["elements"])
assert any(item["role"] == "scroll_container" and "scroll" in item["actions"] for item in data["elements"])
print(path)
PY
