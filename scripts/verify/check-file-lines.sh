#!/usr/bin/env bash
set -euo pipefail

repo_root="$(cd "$(dirname "$0")/../.." && pwd)"
violations=0
while IFS= read -r -d '' file; do
  lines=$(wc -l < "$file" | tr -d ' ')
  if [ "$lines" -gt 500 ]; then
    echo "line-limit violation: $file ($lines lines)"
    violations=1
  fi
done < <(find "$repo_root/services/mac-daemon" "$repo_root/explore/android-daemon-lab" "$repo_root/scripts" -type f \( -name '*.go' -o -name '*.sh' \) -print0)

if [ "$violations" -ne 0 ]; then
  exit 1
fi
