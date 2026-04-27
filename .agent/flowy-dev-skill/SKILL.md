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
2. Read `docs/architecture/collection-workflow-skeleton.md` for the full skeleton state machine and 18 review patches.
3. Read `docs/architecture/module-delivery-playbook.md` for the fixed development / verification / distillation rhythm.
4. Read `./note.md` for recent exploration history, hypotheses, and failed attempts.
5. Read `docs/experiments/` for the current experiment spec and gates.
6. Read `references/evidence-bundle.md` only when defining debug artifacts, transport payloads, or experiment evidence.

## Current Development Phase

**Phase: collection-skeleton implementation \(post-exploration\)**

探索已完成，进入编码实现阶段。优先级：

1. **最小 flow 骨架**：INIT → LIST_ENTRY → PICK_NEXT → ENTER_DETAIL → DETAIL_TASK\(snapshot\) → BACK_TO_LIST → CHECK_CONTINUE → DONE
2. **测试先行**：每加一个 block / flow 片段，先写单测 + fixture，再写实现
3. **逐步加 feature**：snapshot → comments scroll → interactions → dedup → checkpoint/resume
4. **always-on 健壮性**：小错误不影响整体完成，retry ≤ 3，skip on failure

编码顺序固定为：foundation → blocks → flows → integration test。

## Development Tracks

Classify each task into one primary track before editing:

- **foundation**: shared pure utilities, matchers, hash helpers, checkpoint serializer
- **blocks**: smallest capability-level orchestration units \(observe, filter, anchor, operate, scroll-and-collect\)
- **flows**: task-level orchestration and state transitions \(collection workflow skeleton\)
- **transport**: phone agent -> Mac daemon transport, sync, replay, evidence pullback
- **ui-debug**: on-device overlay / debug panel / operator-visible output
- **test**: unit test, fixture, regression, integration

If a request spans multiple tracks, finish the smallest blocking track first.

## Default Debug Loop

Run this loop for nearly every feature:

1. Define the exact observation to prove.
2. Define the evidence bundle before coding.
3. Implement the thinnest end-to-end path.
4. Capture one real run from a device or emulator.
5. Review artifacts on Mac before claiming success.
6. Update `note.md` with hypothesis / what changed / evidence / failure mode / next guess.
7. Promote only stable findings into `AGENTS.md`.

Never skip step 5. No artifact review means the capability is not verified.

## Delivery Rhythm

When the task is no longer a one-off experiment and needs to become repo truth, follow:

```text
scope -> truth -> fixtures -> tests -> implementation -> verify -> evidence -> distill
```

Use `docs/architecture/module-delivery-playbook.md` as the detailed truth source.

Two hard rules:

1. If the repo rule says a gate is required, make it executable in `scripts/verify/` or `scripts/dev/`, not only descriptive in markdown.
2. If a finding is stable enough to guide future work, promote it into `MEMORY.md`, `AGENTS.md`, or this skill instead of leaving it only in chat.

## Coding Layering Rule

When planning or reviewing code in this repo, separate logic into three layers:

- `foundation`: shared pure utilities and helpers
- `blocks`: smallest capability-level orchestration units
- `flows`: task-level orchestration and state transitions

Do not collapse these three layers into one file or one module.

## Test Strategy \(minimum viable\)

每新增一个 module，必须通过五级递进门禁（缺一不可）：

1. **L1 Unit test**: 覆盖正常路径 + 至少一个错误路径
2. **L2 Coverage test**: 每个 block 有 success / error / boundary 覆盖
3. **L3 Orchestration test**: flow 状态机串联跑通 happy path（本地 mock）
4. **L4 Real-device E2E**: 在真实 Android 设备上跑完完整流程，产出 artifact（截图 / response.json / collection-result.json）
5. **Build gate**: 上述四级全部通过后才允许 build

**硬规则：没有 L4 真机端到端验证，不得宣称完成，不得 close bd task。**

L4 证据必须满足以下至少一项：
- `artifacts/` 下有截图路径
- `artifacts/` 下有 collection-result.json
- `note.md` 中有记录的真机观察证据

测试优先于实现：先写 test fixture + test case，再写 block 实现。

## Skeleton Implementation Epic Order

```
Epic 0: foundation 层
  - hash helpers \(title -> item_id\)
  - checkpoint read/write
  - dedup store \(jsonl append + load\)
  - random timing jitter

Epic 1: 最小 flow \(无评论、无互动\)
  - SearchBlock / TimelineBlock → LIST_ENTRY
  - ObservePageBlock → dump + extract
  - FilterBlock → find list items
  - TapBlock + AnchorBlock → ENTER_DETAIL / BACK_TO_LIST
  - SnapshotBlock → DETAIL_TASK \(phase 1 only\)
  - CollectionFlow → wire states together
  - Integration test: search → detail → snapshot → back → next → done

Epic 2: 评论滚动采集
  - ScrollAndCollectBlock
  - bottom detection
  - comment field extraction
  - error recovery \(restart app\)

Epic 3: 去重 + checkpoint/resume
  - DedupStore foundation
  - checkpoint write on each state transition
  - resume from checkpoint in INIT
  - list position recovery

Epic 4: 互动操作
  - like/fav/follow toggle
  - comment like
  - sub-reply expand

Epic 5: 健壮性
  - run timeout
  - graceful stop
  - WS health check on transitions
  - device policy \(keep screen on\)
```

## XHS Selector Rules \(verified\)

- **contentDescription** is the most reliable anchor \(点赞/收藏/评论 buttons\)
- **ImageView.flags.selected** determines interaction state \(NOT Button's selected\)
- **text** for 关注 button, comment usernames, comment content
- **className + parent layout** for comment like buttons \(no contentDescription\)
- **resourceId all obfuscated** — cannot use for targeting

## Build And Gate Rules

- Every code file must stay at or under **500 lines**.
- Every build must automatically run regression tests.
- Keep build/verify/release entrypoints under `scripts/dev`, `scripts/verify`, and `scripts/release`.
- Keep Mac daemon and Android daemon code in separate modules; keep shared schemas/config under `packages/`.

## Truth Sources

- `AGENTS.md`: durable facts, rules, architectural decisions already accepted
- `docs/architecture/*.md`: detailed workspace/module/build/upgrade design
- `note.md`: exploration diary, hypotheses, failed ideas, next experiments
- `.agent/flowy-dev-skill/SKILL.md`: reusable workflow for future Codex sessions

## Reporting Shape

When reporting work in this repo, keep it short and evidence-first:

1. what changed
2. what evidence exists
3. what is still unverified
4. next concrete debug step
