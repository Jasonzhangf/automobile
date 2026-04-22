#!/usr/bin/env bash
set -euo pipefail

repo_root="$(cd "$(dirname "$0")/../.." && pwd)"
gradle_bin="$HOME/.gradle/wrapper/dists/gradle-8.3-all/6en3ugtfdg5xnpx44z4qbwgas/gradle-8.3/bin/gradle"
project_dir="$repo_root/explore/android-daemon-lab"
version_file="$project_dir/config/runtime-version.json"
apk_path="$project_dir/app/build/outputs/apk/debug/app-debug.apk"

before_version="$(python3 - <<'PY' "$version_file"
import json, sys
print(json.load(open(sys.argv[1]))["versionName"])
PY
)"

echo "android-lab build start"
echo "version before: $before_version"

echo "gate: check-file-lines"
"$repo_root/scripts/verify/check-file-lines.sh"

echo "gate: blocks-spec-unit"
"$repo_root/scripts/verify/blocks-spec-unit.sh"

echo "gate: blocks-spec-coverage"
"$repo_root/scripts/verify/blocks-spec-coverage.sh"

cd "$project_dir"
"$gradle_bin" :app:assembleDebug

after_version="$(python3 - <<'PY' "$version_file"
import json, sys
print(json.load(open(sys.argv[1]))["versionName"])
PY
)"

if [ ! -f "$apk_path" ]; then
  echo "missing apk: $apk_path" >&2
  exit 1
fi

echo "version after: $after_version"
echo "apk: $apk_path"
echo "android-lab build ok"
