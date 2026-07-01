# Collection Flow State Machine — Mainline Source

> Auto-generated reference for 9-state FSM.
> Source: `services/mac-daemon/src/flows/collection_flow.go`

## State Diagram

```mermaid
stateDiagram-v2
    [*] --> INIT
    INIT --> LIST_ENTRY : stateInit()
    LIST_ENTRY --> PICK_NEXT : search "搜索" tap + keyword input + submit
    LIST_ENTRY --> PICK_NEXT : timeline (open app directly)
    PICK_NEXT --> ENTER_DETAIL : next item found (not in dedup)
    PICK_NEXT --> DONE : no more items / scroll end
    ENTER_DETAIL --> DETAIL_TASK : detail anchor verified
    ENTER_DETAIL --> BACK_TO_LIST : detail anchor timeout (retry exhausted)
    DETAIL_TASK --> BACK_TO_LIST : detail task complete
    DETAIL_TASK --> BACK_TO_LIST : detail error (skip item)
    BACK_TO_LIST --> CHECK_CONTINUE : list anchor verified
    BACK_TO_LIST --> LIST_ENTRY : list anchor fail (restart app)
    CHECK_CONTINUE --> PICK_NEXT : target count not met, scroll more
    CHECK_CONTINUE --> DONE : target count met or stopped
    DONE --> [*] : buildResult()
    ERROR --> [*] : buildResult() with error
```

## State transition funcs

| State | Method | Lines | Calls |
|-------|--------|-------|-------|
| INIT | `stateInit()` | 148-153 | checkpoint check |
| LIST_ENTRY | `stateListEntry()` | 155-212 | search or timeline |
| PICK_NEXT | `statePickNext()` | 225-259 | ObservePage, MatchNodes, SelectTargets |
| ENTER_DETAIL | `stateEnterDetail()` | 260-303 | TapBlock, AnchorBlock |
| DETAIL_TASK | `stateDetailTask()` | 305-338 | ObservePage, CommentLikeBlock |
| BACK_TO_LIST | `stateBackToList()` | 339-369 | BackBlock, AnchorBlock |
| CHECK_CONTINUE | `stateCheckContinue()` | 360-370 | check target count + scroll |
| DONE | `buildResult()` | 450-473 | finalize |
| ERROR | `buildResult()` | (from main loop) | finalize with error |

## Main call chain (collection flow)

```mermaid
flowchart LR
    HTTP["POST /exp01/collection/run"]
    --> Handler["CollectionRunHandler"]
    --> Lookup["state.Client(deviceID)"]
    --> Ctx["NewFlowContext(session, app, config, profile, dedupPath)"]
    --> Run["fc.Run()"]
    --> Stop["app.SetActiveRun(fc.Stop)"]
    --> Result["json.Encode(result)"]
    Run --> Result
    Run -->|FSM loop| FSM["9 states"]
    FSM --> Tap["blocks.TapBlock"]
    FSM --> Input["blocks.InputTextBlock"]
    FSM --> Observe["blocks.ObservePage"]
    FSM --> Filter["foundation.MatchNodes"]
    FSM --> Anchor["blocks.AnchorBlock"]
    FSM --> Back["blocks.BackBlock"]
    FSM --> Like["blocks.CommentLikeBlock"]
    FSM --> Scroll["blocks.ScrollCollectBlock"]
```

## Resource limits

| Item | Value |
|------|-------|
| Max iterations | targetCount * 5 + 20 |
| Detail timeout | 120s (default) or config.DetailTimeoutMs |
| Run timeout | config.RunTimeoutMs (0 = no limit) |
| Peer detail retries | config.DetailMaxRetries (default in profile) |
| File size | 485 lines (near 500 cap) |

## Verification gates

| Gate | Command | Required before close |
|------|---------|---------------------|
| Go unit tests | `go test ./services/mac-daemon/...` | always |
| 500-line check | `./scripts/verify/check-file-lines.sh` | always |
| L4 device E2E | manual adb + device | **required** before claiming done |
