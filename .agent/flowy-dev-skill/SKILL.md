---
name: flowy-dev-skill
description: Drive Flowy Android visible-agent development, especially when Codex needs to define or execute the repo-local workflow for perception, screenshot capture, phone↔Mac debugging, Accessibility node extraction, evidence collection, or page-recognition iteration. Use when working in this repository on the Android agent-hand runtime, the local Mac daemon, debugging artifacts, or the perception-first development loop.
---

# Flowy Dev Skill

## Overview

Use this skill to keep Flowy development on one stable loop: **phone-side capture and perception -> artifact sync to Mac daemon -> evidence review on Mac -> update repo truth sources -> iterate**.

Treat this repository as an early-stage product lab for a **visible Android automation assistant / Agent hand**, not a hidden-control tool.

## Read Order

1. Read `./AGENTS.md` for project facts and current boundaries.
2. Read `docs/architecture/module-delivery-playbook.md` for the fixed development / verification / distillation rhythm.
3. Read `./note.md` for recent exploration history, hypotheses, and failed attempts.
4. Read `docs/experiments/` for the current experiment spec and gates.
5. Read `references/evidence-bundle.md` only when defining debug artifacts, transport payloads, or experiment evidence.
6. Make the smallest vertical change that improves one step of the perception loop.
7. During exploration, prefer experiment artifacts and thin prototypes; do **not** expand into formal scaffolding before the experiment exit criteria are proven.

## Current Development Focus

Prioritize work in this order unless the user explicitly changes it:

1. **Perception first**: capture current page state from Android.
2. **Screenshot second**: persist screenshots as debug truth.
3. **Normalization third**: turn Accessibility/OCR output into a stable page model.
4. **Operation later**: clicks/gestures are not the current bottleneck.

Default recognition stack:

- Primary: `AccessibilityService` + `AccessibilityNodeInfo`
- Secondary: screenshot capture
- Tertiary: OCR / visual understanding
- Final output: fused `PageState` / command candidates

## Development Tracks

Classify each task into one primary track before editing:

- **transport**: phone agent -> Mac daemon transport, sync, replay, evidence pullback
- **capture**: screenshot capture, active window metadata, timing, trigger path
- **tree**: Accessibility node extraction, normalization, semantic filtering
- **fusion**: node tree + screenshot + OCR alignment
- **ui-debug**: on-device overlay / debug panel / operator-visible output
- **workflow**: repo conventions, artifact storage, experiment loop, review flow

If a request spans multiple tracks, finish the smallest blocking track first.

## Default Debug Loop

Run this loop for nearly every feature:

1. Define the exact observation to prove.
   - Example: "Can the phone send one screenshot + one node tree to the Mac and persist both under one run id?"
2. Define the evidence bundle before coding.
3. Implement the thinnest end-to-end path.
4. Capture one real run from a device or emulator.
5. Review artifacts on Mac before claiming success.
6. Update `note.md` with:
   - hypothesis
   - what changed
   - evidence
   - failure mode / next guess
7. Promote only stable findings into `AGENTS.md`.

Never skip step 5. No artifact review means the capability is not verified.

## Delivery Rhythm

When the task is no longer a one-off experiment and needs to become repo truth, follow:

```text
scope
-> truth
-> fixtures
-> tests
-> implementation
-> verify
-> evidence
-> distill
```

Use `docs/architecture/module-delivery-playbook.md` as the detailed truth source.

Two hard rules:

1. If the repo rule says a gate is required, make it executable in `scripts/verify/` or `scripts/dev/`, not only descriptive in markdown.
2. If a finding is stable enough to guide future work, promote it into `MEMORY.md`, `AGENTS.md`, or this skill instead of leaving it only in chat.

## Debug Contract for Phone <-> Mac

Prefer a transport path that makes Mac-side inspection easy.

Recommended dev order:

1. Start with a **local Mac daemon** that accepts one run bundle.
2. Let the **phone-side agent** push or stream:
   - screenshot
   - active app/package metadata
   - Accessibility node dump / normalized page JSON
   - log excerpt / timing metadata
3. Store each run under a unique run id on the Mac.
4. Make the run reproducible from saved artifacts before expanding features.

Transport preference in dev:

1. `adb reverse` / `adb forward` when possible for deterministic local debugging
2. local-LAN WebSocket / HTTP when direct ADB transport is inconvenient
3. file export fallback only for manual inspection, not as the main loop

Screenshot evidence rule:

- Once Flowy screenshot capture is available in this repo, use **Flowy-originated screenshots** as the default evidence path for debugging and business-flow exploration.
- Do **not** use `adb screencap` as routine evidence for page-state / workflow verification; reserve adb only for installation/bootstrap-class device plumbing when no Flowy screenshot evidence is involved.
- For **third-party business apps**（例如小红书、微博）, exploration and validation must use only **user-like interaction paths**:
  - allowed: `tap`, `scroll`, `input-text`, `back`, `capture-screenshot`, `dump-accessibility-tree`
  - disallowed as business entry/progression: `intent`, `deep-link`, `component` direct-open, and any JS/Auto.js shortcut
  - if a risk-control / security-limit page appears, treat it as valid evidence, record it, and revert the workflow to pure user-path interaction instead of trying another direct-open variant
- On current Oplus / Android 16 real devices, do **not** treat `am force-stop com.flowy.explore` as a harmless restart during accessibility verification; it can clear Flowy's accessibility enabled state and invalidate capability conclusions.
- When Accessibility availability changes, verify both the **real capability** and the **control-plane hello/capabilities projection**; stale `/exp01/clients` data is a separate bug class from capability failure itself.
- On Android 14+/Oplus devices, MediaProjection session setup is order-sensitive: keep it as **grant -> foreground service promote (`mediaProjection`) -> `registerCallback()` -> `createVirtualDisplay()` -> reuse single session**. If the panel only says `not-ready`, inspect logcat first; do not assume permission was denied.
- For app-internal root commands that stream large binary stdout（例如 `screencap -p | cat`）, do not `waitFor()` process exit before consuming stdout; read the stream concurrently first, otherwise the pipe can fill, deadlock, and get misdiagnosed as a missing root binary.
- For workflow post-anchor verification, do not treat one immediate Accessibility observe as ground truth after a page jump; poll boundedly for a fresh snapshot before concluding `POST_ANCHOR_NOT_MATCHED`.

Upgrade verification reminder:

- For Android dev-lab talking to a remote daemon over `ws://` / `http://`, verify cleartext policy on the real phone first; localhost-only allowlists are not enough once the host moves to LAN/Tailscale.
- For APK in-app upgrade, verify the final installed `versionName/versionCode`; some OEM ROMs may return to Home immediately after the installer intent instead of leaving the installer UI on screen.

## Recognition Workflow

When implementing page recognition, use this order:

1. Acquire active-window Accessibility root.
2. Extract raw nodes with ids, text, desc, class, bounds, enabled/clickable/editable/scrollable flags.
3. Save the raw dump.
4. Capture screenshot from the same run when possible.
5. Normalize to a stable intermediate model.
6. Add OCR/vision only where Accessibility is missing or ambiguous.
7. Generate command candidates only after the page model is inspectable.

Do not jump directly from screenshot to final commands if raw node evidence is available.

## Truth Sources

Use repository documents with strict ownership:

- `AGENTS.md`: durable facts, rules, architectural decisions already accepted
- `docs/architecture/*.md`: detailed workspace/module/build/upgrade design
- `note.md`: exploration diary, hypotheses, failed ideas, next experiments
- `.agent/flowy-dev-skill/SKILL.md`: reusable workflow for future Codex sessions

If a fact is still experimental, keep it in `note.md`, not `AGENTS.md`.

## Reporting Shape

When reporting work in this repo, keep it short and evidence-first:

1. what changed
2. what evidence exists
3. what is still unverified
4. next concrete debug step

## Reference

- For artifact schema, run folders, and minimum evidence payload, read `references/evidence-bundle.md`.

## Build And Gate Rules

Treat these as hard requirements:

- Every code file must stay at or under **500 lines**. Documentation is not bound by this limit.
- Every build must automatically run regression tests. No compile-only success is acceptable.
- Keep build/verify/release entrypoints under `scripts/dev`, `scripts/verify`, and `scripts/release`.
- Keep Mac daemon and Android daemon code in separate modules; keep shared schemas/config under `packages/`.
- For block work, require: unit tests -> coverage tests -> build integration, and only then move to flow orchestration.

## Exploration-First Rule

Current repo phase is exploration-first:

- Prove one experiment with evidence before promoting it into shared scaffolding.
- Keep exploratory code, notes, and artifacts separate from formal app construction.
- Do not build the baseline app/framework layers just because the target structure is already designed.

## Layering Rule

When planning or reviewing code in this repo, separate logic into three layers:

- `foundation`: shared pure utilities and helpers
- `blocks`: smallest capability-level orchestration units
- `flows`: task-level orchestration and state transitions

Do not collapse these three layers into one file or one module.

## Version Rule

Assume the Android-side build version starts at `0.1.0001` and the 4-digit build number must auto-bump on every compile. Treat this as a required build gate, not a release-time manual step.
