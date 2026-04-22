#!/usr/bin/env bash
set -euo pipefail

port="${1:-8787}"
serial="${ANDROID_SERIAL:-}"

if ! command -v adb >/dev/null 2>&1; then
  echo "adb not found" >&2
  exit 1
fi

if [ -n "$serial" ]; then
  device_args=(-s "$serial")
else
  mapfile -t devices < <(adb devices | awk 'NR>1 && $2=="device" {print $1}')
  if [ "${#devices[@]}" -eq 0 ]; then
    echo "no connected android device" >&2
    exit 1
  fi
  if [ "${#devices[@]}" -gt 1 ]; then
    echo "multiple devices connected; set ANDROID_SERIAL explicitly" >&2
    printf 'devices: %s\n' "${devices[@]}" >&2
    exit 1
  fi
  device_args=(-s "${devices[0]}")
fi

adb "${device_args[@]}" reverse "tcp:${port}" "tcp:${port}"
adb "${device_args[@]}" reverse --list
