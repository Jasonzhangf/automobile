#!/usr/bin/env bash
set -euo pipefail
repo_root="$(cd "$(dirname "$0")/../.." && pwd)"
apk="$repo_root/explore/android-daemon-lab/app/build/outputs/apk/debug/app-debug.apk"
if [ ! -f "$apk" ]; then
  "$repo_root/scripts/dev/phase-a-build-android-lab.sh"
fi
adb install -r "$apk"
