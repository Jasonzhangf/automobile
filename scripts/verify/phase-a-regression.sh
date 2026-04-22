#!/usr/bin/env bash
set -euo pipefail

repo_root="$(cd "$(dirname "$0")/../.." && pwd)"
"$repo_root/scripts/verify/check-file-lines.sh"
cd "$repo_root/services/mac-daemon"
mkdir -p .tmp
go test ./...
go build -o ./.tmp/flowy-mac-daemon ./src
