#!/usr/bin/env bash
set -euo pipefail
repo_root="$(cd "$(dirname "$0")/../.." && pwd)"
gradle_bin="$HOME/.gradle/wrapper/dists/gradle-8.3-all/6en3ugtfdg5xnpx44z4qbwgas/gradle-8.3/bin/gradle"
"$repo_root/scripts/verify/check-file-lines.sh"
cd "$repo_root/explore/android-daemon-lab"
"$gradle_bin" :app:testDebugUnitTest
