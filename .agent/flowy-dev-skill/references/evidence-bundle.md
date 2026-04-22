# Evidence Bundle Reference

## Purpose

Use one run bundle as the smallest verifiable debug unit for Flowy perception work.

## Recommended run directory

```text
artifacts/
  YYYY-MM-DD/
    <run-id>/
      manifest.json
      screenshot.png
      page-state.json
      accessibility-raw.json
      logs.txt
```

`<run-id>` should include timestamp + short purpose, for example:

```text
2026-04-21T14-30-12_capture-home-screen
```

## Minimum manifest fields

```json
{
  "runId": "2026-04-21T14-30-12_capture-home-screen",
  "capturedAt": "2026-04-21T14:30:12+08:00",
  "device": {
    "model": "...",
    "androidVersion": "..."
  },
  "app": {
    "packageName": "...",
    "activity": "..."
  },
  "transport": "adb-reverse|websocket|http|manual-export",
  "artifacts": {
    "screenshot": "screenshot.png",
    "pageState": "page-state.json",
    "accessibilityRaw": "accessibility-raw.json",
    "logs": "logs.txt"
  }
}
```

## Minimum evidence standard

A run is considered usable only if it includes:

1. a screenshot or explicit reason why screenshot is unavailable
2. package/activity metadata
3. either raw Accessibility dump or explicit failure reason
4. normalized page-state output or explicit failure reason
5. log excerpt with timestamps

## Page-state baseline

Start with a narrow, inspectable structure:

```json
{
  "app": {
    "packageName": "com.example",
    "activity": "MainActivity"
  },
  "screen": {
    "width": 1080,
    "height": 2400,
    "rotation": 0
  },
  "nodes": [],
  "capturedAt": "2026-04-21T14:30:12+08:00"
}
```

Do not overdesign the schema before one real end-to-end run exists.

## Review checklist

Before claiming a debug step works, verify:

- Does one run id map to one coherent capture event?
- Are screenshot and node dump from the same moment?
- Can the run be inspected on Mac without touching the phone again?
- Can a later agent replay reasoning from saved artifacts alone?
