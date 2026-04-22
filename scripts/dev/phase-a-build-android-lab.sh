#!/usr/bin/env bash
set -euo pipefail
repo_root="$(cd "$(dirname "$0")/../.." && pwd)"
"$repo_root/scripts/dev/build-android-lab.sh"
