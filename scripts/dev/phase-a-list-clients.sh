#!/usr/bin/env bash
set -euo pipefail

base_url="${FLOWY_MAC_DAEMON_BASE_URL:-http://127.0.0.1:8787}"
curl -sSf "$base_url/exp01/clients" | python3 -m json.tool
