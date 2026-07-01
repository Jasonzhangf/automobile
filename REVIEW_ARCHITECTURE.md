# Flowy Code Architecture Review

> 时间: 2026-07-01
> Reviewer: /root
> 范围: ~/Documents/github/flowy-code (Jasonzhangf/automobile)
> 复盘对象: 整体代码结构、function map、模块化、共享函数化、mainline source

---

## 一、整体目录结构问题 (Severity: HIGH)

### 1.1 packages/ 真源空壳

约定结构（AGENTS.md 描述）:
- `packages/foundation` — 共享纯函数库
- `packages/blocks` — 最小能力块
- `packages/flows` — 任务编排
- `packages/protocol` — 共享协议
- `packages/page-model` — 共享页面执行模型
- `packages/config` — 共享配置

实际状况:
- 上述 6 个 packages/ 目录全部仅含 `README.md`，**没有任何可消费代码**
- 实际代码全部位于 `services/mac-daemon/src/{foundation,blocks,flows,proto,state}`
- packages/ 承诺的"多端共享真源"从未兑现

**违反规则**:
- 硬护栏 #18（功能定位唯一真源原则）
- 硬护栏 #21（重复实现物理禁止原则）
- 硬护栏 #22（测试映射显式化原则）
- Hard Guard #27（代码绑定式 function map 原则）

### 1.2 双侧独立实现 (Kotlin blocks vs Go blocks)

现状:
- Mac 编排侧：`services/mac-daemon/src/{foundation,blocks,flows,proto,state}` (Go)
- Android 执行侧：`explore/android-daemon-lab/.../{foundation,blocks,flows,ui,runtime}` (Kotlin)
- 两侧各实现了**同名不同语义的 capability**：
  - Mac 有 `anchor_spec.go` (6 conditions)，Android 有 `EvaluateAnchorBlock.kt` (运行期 evaluate)
  - Mac 有 `filter_match.go` (MatchNodes + SelectTargets)，Android 有 `FilterTargetsBlock.kt` (snapshot + selector)
  - 两侧各自定义 `OperationBackend` (accessibility/root)

**违反规则**:
- 硬护栏 #21（重复实现物理禁止）
- 硬护栏 #22（Rust 迁移方向显式化原则）— 应当是单源 spec + 跨语言映射
- AGENTS.md 描述的"无头模式（Kotlin 重写）"也尚未启动

---

## 二、500 行硬门禁问题 (Severity: MEDIUM)

| 文件 | 行数 | 风险 |
|---|---|---|
| `services/mac-daemon/src/flows/collection_flow.go` | 485 | 距 500 行门禁仅 15 行缓冲 |
| `explore/.../ui/workbench/WorkbenchOverlayService.kt` | 435 | 距门禁 65 行 |
| `services/mac-daemon/src/blocks/operation_block.go` | 264 | 单文件 4 个 operation + resolve helpers |

collection_flow.go 在添加任何新状态或 field 时会立即超限，必须按"两层编排 + helper 文件"模式拆解。

---

## 三、重复实现与下沉问题 (Severity: HIGH)

### 3.1 collection_flow.go 内部辅助方法未下沉

`collection_flow.go` 包含 18 个方法，其中应下沉的:
- `makeCommand()` — 构造 `proto.CommandEnvelope`（应在 `blocks/command_helpers.go`）
- `computeItemID()` — 算 item_id（应在 `foundation/dedup_store.go` 或 helper）
- `jitterSleep()` — 随机延迟（已在 `foundation/timing.go` 但 collection_flow 自己又写一遍）
- `sendOpenDeepLink()` / `sendPressKey()` — 应走 `blocks/operation_block.go`
- `anchorCheck()` — 应走 `blocks/anchor_block.go`

**违反规则**:
- 硬护栏 #22（函数库优先与编排层纯度原则）

### 3.2 operation_block.go 内部重复 observe+filter

`operation_block.go::resolveTapPoint()` 内部:
1. 调 `ObservePage()` 拿 nodes
2. 手动遍历 nodes 比对 `DescContains`/`TextContains`
3. 取 bounds center

但 `foundation/filter_match.go` 已实现 `MatchNodes` + `SelectTargets`，未复用。

**违反规则**:
- 硬护栏 #25（函数库优先与编排层纯度原则）
- 硬护栏 #21（重复实现物理禁止）

### 3.3 blocks 层同时依赖 state 和 proto

`blocks/*.go` 全部:
- 直接接收 `*state.ClientSession` + `*state.AppState`
- 直接构造 `proto.CommandEnvelope`

理想: blocks 只接收 `(transport, command, payload) -> error` 抽象
当前: blocks 知道 session 锁/pending channel/request id 序号生成 等细节

**违反规则**:
- 硬护栏 #22（函数库优先与编排层纯度原则）

---

## 四、Pipeline 类型与节点编号问题 (Severity: HIGH)

### 4.1 缺少 Pipeline 唯一类型锁定

`proto/types.go` 定义:
- `CommandEnvelope` (requestId, runId, command, payload)
- `ResponseEnvelope` (status, error, artifacts, inlineLogs)
- `ErrorObject` (code, message, details)

但**没有按方向 + 节点序号 + 节点语义**定义 pipeline:
- `HubReq01CommandReceived` / `HubReq02CommandDispatched` / `HubReq03CommandExecuted`
- `HubResp01ResponseReady` / `HubResp02ResponseDelivered` / `HubResp03ResponseArchived`

每个节点当前用自由结构拼接，无类型不可接 + 运行时必拦。

**违反规则**:
- 硬护栏 #17（Pipeline 唯一类型锁定原则）

### 4.2 主线调用链未绑定代码

需求（AGENTS.md 硬护栏 #29）：mainline call map 必须把 request/response/error 主线节点 -> caller -> callee -> owner feature 绑到真实代码。

当前：
- 只有 `main.go` 里 8 个 mux.Handle
- 无 caller/callee 链文档
- 无 `binding pending` 标记

---

## 五、function map 与 owner registry 缺失 (Severity: HIGH)

### 5.1 无 feature/owner registry

需求（AGENTS.md 硬护栏 #18）：每个关键功能必须明确 feature_id、唯一 owner 模块、允许修改路径、禁止修改路径、必跑验证命令、关联红测。

当前：
- 没有 `docs/feature-map.md`
- 没有 `docs/owner-registry.md`
- 任何功能定位只能靠 grep

### 5.2 无 function map

需求（硬护栏 #27）：长期项目必须有可绑定代码的 function map。

当前：
- 无 `docs/function-map.md`
- 无 entry symbols / file paths
- 无 `binding pending` 标记

---

## 六、Wiki / Manifest 缺失 (Severity: MEDIUM)

需求（硬护栏 #28）：长期项目必须落 wiki review 面 + machine-readable manifest。

当前：
- `docs/architecture/` 存在但**只描述，不绑定代码**
- 无 `wiki/manifest.json` 或 `lifecycle.json`
- 无 HTML 渲染的可点击 review surface

---

## 七、测试与门禁问题 (Severity: MEDIUM)

### 7.1 L1-L4 门禁散落在多处

- AGENTS.md 写 L1→L2→L3→L4→build
- `docs/architecture/module-delivery-playbook.md` §3.6 写五级门禁
- `.agent/flowy-dev-skill/SKILL.md` 也写门禁

**问题**: 三处真源必须保持同步，目前缺少 gate 自动化校验。

### 7.2 单元测试存在但 L4 真机 E2E 缺

- Go 单测：58 pass + 1 skip (real dump fixture)
- Kotlin 单测：覆盖未知
- 端到端：未验证（adb devices 为空 + mobile 待授权）

---

## 八、修复优先级建议

### P0 (立即):
1. 兑现 packages/ 真源 — 把 Mac 侧 foundation/blocks 物理迁入 packages/，并提供 Kotlin stub
2. 拆解 collection_flow.go 超过 500 行风险 — 抽出 helper 文件
3. operation_block.go::resolveTapPoint 复用 foundation/filter_match.go

### P1 (本 Epic):
4. 建立 feature/owner registry + function map (docs/feature-map.md, docs/function-map.md)
5. 建立 mainline call map (docs/mainline-call-map.md)
6. Pipeline 节点编号 + 类型锁定 (proto/types.go 拆分节点)
7. blocks 解耦 state/proto (注入 transport interface)

### P2 (后续):
8. Kotlin blocks 接入 packages/ 真源（消除双侧独立实现）
9. wiki/manifest.json + HTML 渲染
10. 自动化 gate 校验 AGENTS/MEMORY/SKILL 三处门禁同步


---

## 九、RouteCodex 质量工程体系参考 (2026-07-01)

RouteCodex (`~/github/routecodex`) 是同类多层架构项目，其质量工程体系如下：

### 9.1 已落文件 (全部机器可读)

| 文件 | 格式 | 内容 |
|------|------|------|
| `docs/architecture/function-map.yml` | YAML | feature_id + owner + canonical_types/builders + allowed/forbidden paths + required gates |
| `docs/architecture/mainline-call-map.yml` | YAML | chain_id + edges (from/to + caller/callee + binding status) + 多个 mainline |
| `docs/architecture/verification-map.yml` | YAML | feature_id -> change_risk -> unit/contract/integration/smoke/build tests |
| `docs/architecture/mainline-manifests/*.mainline.yml` | YAML | 每个 mainline 链独立 YAML |
| `docs/architecture/wiki/*.md` | Markdown + Mermaid | 可点击 review surface (HTML 渲染) |
| `docs/architecture/wiki/html/*.html` | HTML | 自动生成，不可手动编辑 |

### 9.2 硬护栏对比

| 规则 | RouteCodex | Flowy |
|------|------------|-------|
| 单一路径真源 | 20 条硬护栏 | 已在 AGENTS.md 但无结构化 |
| Pipeline 节点编号 | `<Module><Phase><NN><Node>` | 无 |
| 错误链 | ErrorErr01-06 | 无独立错误链 |
| 红测 | `tests/red-tests/` | 无 |
| 绑定状态标记 | `binding pending` / `partial` / `anchored` | 无 |
| no fallback | 明确禁止 | 明确禁止 |
| 类型拓扑锁 | 类型不可接 + 运行时必拦 + 导出不可见 | 无 |
| 修改前必查 | function-map -> mainline -> wiki -> test -> code | 无此流程 |
| 架构 review | 验证后必须做 | 首次做 (本文) |

### 9.3 Flowy 需补充的工程保障

1. **YAML 化**：当前 function-map.md 是自由文本，需转为 function-map.yml (已创建)
2. **mainline call map YAML**：已创建
3. **verification map YAML**：已创建
4. **500 行门禁**：已有 shell script 但无自动化 gate
5. **红测**：需新建 `tests/red-tests/` 目录+首批红测
6. **binding pending 标记**：已在 mainline-call-map.yml 标记
7. **Packages/ 兑现**：P0 修复项
8. **双端 schema 同步门禁**：Go ↔ Kotlin 协议一致性校验脚本
