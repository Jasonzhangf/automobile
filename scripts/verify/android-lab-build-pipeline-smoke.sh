#!/usr/bin/env bash
set -euo pipefail

repo_root="$(cd "$(dirname "$0")/../.." && pwd)"
version_file="$repo_root/explore/android-daemon-lab/config/runtime-version.json"

before_build_number="$(python3 - <<'PY' "$version_file"
import json, sys
print(json.load(open(sys.argv[1]))["buildNumber"])
PY
)"

"$repo_root/scripts/dev/build-android-lab.sh" >/tmp/flowy-android-lab-build.log
cat /tmp/flowy-android-lab-build.log

after_build_number="$(python3 - <<'PY' "$version_file"
import json, sys
print(json.load(open(sys.argv[1]))["buildNumber"])
PY
)"

expected=$((before_build_number + 1))
if [ "$after_build_number" -ne "$expected" ]; then
  echo "build number mismatch: before=$before_build_number after=$after_build_number expected=$expected" >&2
  exit 1
fi

echo "android-lab build pipeline smoke ok"
