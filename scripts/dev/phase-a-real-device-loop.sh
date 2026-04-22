#!/usr/bin/env bash
set -euo pipefail

repo_root="$(cd "$(dirname "$0")/../.." && pwd)"
cat <<MSG
Phase A real-device loop:
1. Start Mac daemon in one shell:
   $repo_root/scripts/dev/phase-a-start-mac-daemon.sh
2. Connect one Android device via adb.
3. Prepare reverse tunnel:
   $repo_root/scripts/dev/phase-a-adb-reverse.sh
4. Build APK:
   $repo_root/scripts/dev/phase-a-build-android-lab.sh
5. Install APK:
   $repo_root/scripts/dev/phase-a-install-android-lab.sh
6. On device, open 'Flowy Daemon Lab' and tap Start Daemon.
7. Confirm client hello:
   $repo_root/scripts/dev/phase-a-list-clients.sh
8. Dispatch ping:
   $repo_root/scripts/dev/phase-a-send-ping.sh
9. Dispatch fetch-logs:
   $repo_root/scripts/dev/phase-a-send-fetch-logs.sh
MSG
