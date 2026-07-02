# Function Map (Mac Daemon Side)

> True source: code symbols + file paths.
> Last updated: 2026-07-01
> Status: Established

## Go module: `flowy/services/mac-daemon`

---

## 1. Entrypoints (HTTP Routes)

| Route | Method | Handler | Layer | Owner File |
|-------|--------|---------|-------|------------|
| `/health` | GET | inline | main | `src/main.go:25` |
| `/exp01/clients` | GET | `app.Clients()` | main | `src/main.go:26` |
| `/exp01/ws` | WebSocket | `blocks.WSAcceptHandler` → `flows.RunClientSession` | blocks→flows | `src/blocks/ws_accept.go`, `src/flows/client_session_flow.go` |
| `POST /exp01/command` | POST | `flows.CommandRoundtripHandler` | flows | `src/flows/command_roundtrip_flow.go` |
| `POST /exp01/artifacts` | POST | `flows.ArtifactUploadHandler` | flows | `src/flows/artifact_upload_flow.go` |
| `POST /exp01/collection/run` | POST | `flows.CollectionRunHandler` | flows | `src/flows/collection_run_handler.go` |
| `POST /exp01/collection/stop` | POST | inline | main | `src/main.go:38` |
| `GET /flowy/upgrade/check` | GET | `flows.UpgradeCheckHandler` | flows | `src/flows/upgrade_apk_manifest_flow.go` |
| `GET /flowy/upgrade/apk` | GET | `flows.UpgradeApkManifestHandler` | flows | `src/flows/upgrade_apk_manifest_flow.go` |
| `GET /flowy/upgrade/apk/download` | GET | `http.ServeFile` | main | `src/main.go:29` |

## 2. Layers

### 2a. foundation (pure functions, no session/state deps)

| Symbol | Kind | File | Description |
|--------|------|------|-------------|
| `EvaluateAnchor(nodes, spec)` | func | `src/foundation/anchor_spec.go:28` | 6-condition anchor check |
| `AnchorResult` | struct | `src/foundation/anchor_spec.go:18` | EvaluateAnchor output |
| `NewBottomDetector()` | func | `src/foundation/bottom_detector.go:39` | Create scroll-bottom detector |
| `BottomDetector.Check(nodes)` | method | `src/foundation/bottom_detector.go:47` | Check if bottom reached (3 signals) |
| `WriteCheckpoint(dir, cp)` | func | `src/foundation/checkpoint.go:49` | Persist flow checkpoint |
| `ReadCheckpoint(dir)` | func | `src/foundation/checkpoint.go:64` | Load flow checkpoint |
| `DeleteCheckpoint(dir)` | func | `src/foundation/checkpoint.go:81` | Remove checkpoint |
| `CommentExtractor` | (module) | `src/foundation/comment_extractor.go` | Parse comment nodes from tree |
| `LoadDedupStore(path)` | func | `src/foundation/dedup_store.go:28` | Load or create dedup store |
| `FilePaths` | (module) | `src/foundation/file_paths.go` | RunDir/ManifestPath/... helpers |
| `MatchNodes(nodes, spec)` | func | `src/foundation/filter_match.go:10` | Filter nodes by FilterSpec |
| `SelectTargets(matches, strategy)` | func | `src/foundation/filter_match.go:30` | Apply selection strategy |
| `FilterSpec` | struct | `src/foundation/filter_spec.go` | 15-field filter spec |
| `ItemIDFromTitle(title)` | func | `src/foundation/hash_helpers.go:11` | Dedup item ID from title |
| `NewRequestID(now, seq)` | func | `src/foundation/ids.go:12` | Generate request ID |
| `NewRunID(now, cmd, purpose)` | func | `src/foundation/ids.go:16` | Generate run ID |
| `Decode[T](data)` | func | `src/foundation/json_codec.go:5` | Generic JSON decode |
| `EncodePretty(value)` | func | `src/foundation/json_codec.go:11` | Pretty-print JSON |
| `DecodeArtifactUpload(r)` | func | `src/foundation/multipart_artifact.go:14` | Parse multipart upload |
| `SHA256Hex(content)` | func | `src/foundation/sha256.go:8` | SHA256 hex string |
| `RandomDuration(min, max)` | func | `src/foundation/timing.go:10` | Random jitter duration |
| `OperationDelay()` | func | `src/foundation/timing.go:26` | Default operation jitter |
| `UiNode` | struct | `src/foundation/ui_node.go` | UI node data structure |
| `CompareVersion(l, r)` | func | `src/foundation/version_compare.go:8` | Compare version strings |

### 2b. blocks (capability modules, know session+proto)

| Symbol | Kind | File | Description |
|--------|------|------|-------------|
| `WSAcceptHandler(runClient)` | func | `src/blocks/ws_accept.go:11` | HTTP→WS upgrader |
| `CommandRoundtrip(session, app, cmd)` | func | `src/blocks/command_roundtrip.go:14` | Send WS cmd + wait response |
| `SendCommand(session, cmd)` | func | `src/blocks/send_command.go:8` | Write WS message |
| `ObservePage(session, app, root, timeout)` | func | `src/blocks/observe_page.go:27` | dump-ui-tree-root → parse result |
| `ObserveResult` | struct | `src/blocks/observe_page.go:17` | ObservePage output |
| `AnchorBlock(session, app, root, spec, cfg)` | func | `src/blocks/anchor_block.go:42` | Bounded-poll anchor check |
| `TapBlock(session, app, root, target, backend, timeout)` | func | `src/blocks/operation_block.go:31` | Tap at resolved point |
| `ScrollBlock(session, app, root, target, dir, backend, timeout)` | func | `src/blocks/operation_block.go:82` | Scroll at resolved point |
| `BackBlock(session, app, backend, timeout)` | func | `src/blocks/operation_block.go:127` | Press back key |
| `InputTextBlock(session, app, text, backend, timeout)` | func | `src/blocks/operation_block.go:160` | Send text input |
| `DetectCommentLikes(nodes)` | func | `src/blocks/comment_like_block.go:31` | Parse like/fav button states |
| `CommentLikeBlock(...)` | func | `src/blocks/comment_like_block.go:76` | Execute like/fav action |
| `ToggleActionBlock(...)` | func | `src/blocks/toggle_action_block.go:39` | Generic toggle tap |
| `ScrollCollectBlock(...)` | func | `src/blocks/scroll_collect_block.go:43` | Scroll + collect visible items |
| `HealthCheck(session, app, timeout)` | func | `src/blocks/recovery_block.go:39` | WS ping health check |
| `RecoverSession(cfg)` | func | `src/blocks/recovery_block.go:108` | Full session recovery |
| `PersistManifest(dir, manifest)` | func | `src/blocks/persist_manifest.go:10` | Write run manifest |
| `PersistResponse(dir, response)` | func | `src/blocks/persist_response.go:10` | Write response to file |
| `PersistLogsArtifact(dir, entries)` | func | `src/blocks/persist_logs_artifact.go:13` | Write logs to file |

### 2c. flows (orchestration)

| Symbol | Kind | File | Description |
|--------|------|------|-------------|
| `RunClientSession(app, conn)` | func | `src/flows/client_session_flow.go:27` | WS session lifecycle |
| `CollectionRunHandler(app)` | func | `src/flows/collection_run_handler.go:21` | POST handler for collection run |
| `NewFlowContext(...)` | func | `src/flows/collection_flow.go:54` | Create flow context |
| `FlowContext.Run()` | method | `src/flows/collection_flow.go:82` | 9-state FSM |
| `FlowContext.Stop()` | method | `src/flows/collection_flow.go:476` | Graceful stop |
| `FlowContext.stateXxx()` | 9 methods | `src/flows/collection_flow.go:148-367` | State handlers |
| `CommandRoundtripHandler(app)` | func | `src/flows/command_roundtrip_flow.go:14` | POST handler for manual cmd |
| `ArtifactUploadHandler(app)` | func | `src/flows/artifact_upload_flow.go:14` | POST handler for artifact |
| `FinalizeRun(root, response)` | func | `src/flows/finalize_run_flow.go:11` | Write response+manifest |
| `UpgradeCheckHandler(...)` | func | `src/flows/upgrade_apk_manifest_flow.go:16` | GET version check |
| `UpgradeApkManifestHandler(...)` | func | `src/flows/upgrade_apk_manifest_flow.go:40` | GET APK manifest |

### 2d. proto (protocol types)

| Symbol | Kind | File | Description |
|--------|------|------|-------------|
| `CommandEnvelope` | struct | `src/proto/types.go:13` | WS command |
| `ResponseEnvelope` | struct | `src/proto/types.go:58` | WS response |
| `ErrorObject` | struct | `src/proto/types.go:34` | Error details |
| `LogEntry` | struct | `src/proto/types.go:48` | Log entry |
| `ArtifactDescriptor` | struct | `src/proto/types.go:40` | Artifact metadata |
| `ClientHello` | struct | `src/proto/types.go:3` | Device hello |
| `DeviceMeta` / `AppMeta` | struct | `src/proto/types.go:23/29` | Device/app info |
| `RunManifest` | struct | `src/proto/types.go:75` | Run manifest |
| `UpgradeCheckResponse` | struct | `src/proto/types.go:110` | Upgrade response |

### 2e. state (runtime state)

| Symbol | Kind | File | Description |
|--------|------|------|-------------|
| `New(artifactRoot)` | func | `src/state/state.go:36` | Create app state |
| `ClientSession` | struct | `src/state/state.go:10` | WS client session |
| `AppState` | struct | `src/state/state.go:16` | Runtime state |
| `RegisterClient/Client/Clients` | methods | `src/state/state.go:48-84` | Client registry |
| `RegisterPending/ResolvePending` | methods | `src/state/state.go:87-103` | Pending command map |
| `ActiveRun/StopActiveRun` | methods | `src/state/state.go:114-148` | Run lifecycle |

