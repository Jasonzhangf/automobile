#!/usr/bin/env bash
set -euo pipefail

repo_root="$(cd "$(dirname "$0")/../.." && pwd)"
"$repo_root/scripts/verify/phase-a-regression.sh"
cd "$repo_root/services/mac-daemon"
exec go run ./src
