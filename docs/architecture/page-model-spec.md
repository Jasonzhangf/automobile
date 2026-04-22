# Page Model Spec v1

## 目标

定义 Flowy 的共享页面执行模型真源。

这份真源服务于：

- CLI / WebSocket 控制面
- Android runtime
- Mac / remote control daemon
- blocks / flows
- regression fixtures

第一版只先定 5 个核心对象：

1. `OperationSpec`
2. `FilterSpec`
3. `AnchorSpec`
4. `WorkflowStepSpec`
5. `PageState`

---

## 一、设计原则

### 1. 先共享 schema，再平台实现

所有平台都只消费同一套 page model，不各自定义。

### 2. 先描述意图，再描述 transport

`page-model` 只表达：

- 要做什么
- 要找什么
- 怎么验证

它不负责描述 WebSocket/CLI 传输细节。

### 3. 先单步，再多步

第一版只保证单步 workflow 可表达、可验证、可回放。

### 4. PageState 是观察真源

所有 filter / anchor / workflow-step 都建立在 `PageState` 之上。

---

## 二、目录结构

建议固定为：

```text
packages/page-model/
├─ README.md
└─ examples/
   ├─ operation.tap.example.json
   ├─ filter.search-entry.example.json
   ├─ anchor.open-search.example.json
   └─ workflow-step.open-search.example.json
```

---

## 三、OperationSpec

`OperationSpec` 只描述原子操作，不描述长流程。

第一版允许：

- `tap`
- `long_press`
- `scroll`
- `input_text`
- `back`
- `home`
- `wait`

最小模型：

```json
{
  "schemaVersion": "flowy-page-model-operation-v1",
  "operationId": "op_open_xhs_search",
  "kind": "tap",
  "targetRef": "target:search_button",
  "args": {},
  "timeoutMs": 3000
}
```

字段：

- `operationId`：本次操作标识
- `kind`：操作类型
- `targetRef`：引用 filter 或 workflow 选中的目标
- `args`：附加参数
- `timeoutMs`：超时

`scroll` 的 `args` 建议：

```json
{ "direction": "forward", "distance": "page" }
```

`input_text` 的 `args` 建议：

```json
{ "text": "<TEXT>", "clearBefore": true }
```

---

## 四、FilterSpec

`FilterSpec` 描述“当前流程要从 PageState 里关心什么”。

第一版固定 6 类：

- `actionable`
- `scrollable`
- `input`
- `card_like`
- `image_like`
- `blocker`

最小模型：

```json
{
  "schemaVersion": "flowy-page-model-filter-v1",
  "filterId": "search_entry_filter",
  "kind": "actionable",
  "match": {
    "anyTexts": ["搜索"],
    "roles": ["button", "action", "input"]
  },
  "select": {
    "strategy": "first_best"
  }
}
```

字段：

- `filterId`：filter 标识
- `kind`：filter 类型
- `match`：匹配条件
- `select`：选取策略

第一版 `match` 支持：

- `anyTexts`
- `allTexts`
- `roles`
- `viewIds`
- `withinPageSignatures`
- `visibleOnly`

第一版 `select.strategy` 支持：

- `first_best`
- `top_most`
- `deepest_visible`
- `largest_card`

---

## 五、AnchorSpec

`AnchorSpec` 用于保证状态机不跑偏。

它分两部分：

- `preAnchor`
- `postAnchor`

最小模型：

```json
{
  "schemaVersion": "flowy-page-model-anchor-v1",
  "anchorId": "open_xhs_search_anchor",
  "preAnchor": {
    "mustContainTexts": ["搜索", "发现"],
    "targetVisible": true
  },
  "postAnchor": {
    "anyOf": [
      { "pageTitle": "问ai" },
      { "mustContainTexts": ["猜你想搜", "历史记录"] }
    ]
  }
}
```

第一版支持的条件：

- `pageSignature`
- `pageTitle`
- `mustContainTexts`
- `mustNotContainTexts`
- `targetVisible`
- `targetBoundsStable`
- `targetCountRange`

规则：

- `preAnchor` 用来验证“下手前是否可信”
- `postAnchor` 用来验证“执行后是否到达目标态”

---

## 六、WorkflowStepSpec

`WorkflowStepSpec` 是单步任务真源。

它固定串联：

```text
observe
-> filter
-> pre-anchor
-> operation
-> observe
-> post-anchor
```

最小模型：

```json
{
  "schemaVersion": "flowy-page-model-workflow-step-v1",
  "workflowStepId": "open_xhs_search",
  "name": "Open XHS Search",
  "observerSpec": {
    "requireAccessibility": true,
    "requireScreenshot": false
  },
  "targetFilterRef": "filter:search_entry_filter",
  "operationRef": "operation:tap_search_entry",
  "anchorRef": "anchor:open_xhs_search_anchor",
  "retryPolicy": {
    "maxAttempts": 1
  }
}
```

字段：

- `workflowStepId`
- `name`
- `observerSpec`
- `targetFilterRef`
- `operationRef`
- `anchorRef`
- `retryPolicy`

第一版 `observerSpec` 支持：

- `requireAccessibility`
- `requireScreenshot`
- `requiredFilters`

---

## 七、PageState

`PageState` 是 observer 归一化后的统一页面真源。

第一版至少包含：

- `app.packageName`
- `page.title`
- `page.signature`
- `anchors`
- `elements`
- `page.stats`

规则：

- filter 只读 `PageState`
- anchor 只读 `PageState`
- workflow-step 不直接读 raw accessibility tree

---

## 八、对象引用规则

为了让 blocks / flows 共享同一套真源，第一版统一采用 `ref` 方式：

- `filter:search_entry_filter`
- `operation:tap_search_entry`
- `anchor:open_xhs_search_anchor`

这样：

- blocks 可以按 ref 查对象
- CLI / WS 也可以按 ref 下发 step

---

## 九、第一批典型样本

第一批推荐固定 4 个样本：

1. 小红书打开搜索
2. 京东跳过开屏广告
3. 微博点击评论按钮
4. 设置页点击 row-card

当前先落第 1 个：

- `open_xhs_search`

---

## 十、当前接受的设计结论

1. `PageState` 是观察真源。
2. `FilterSpec` 只表达筛选逻辑，不表达操作。
3. `AnchorSpec` 只表达状态约束，不表达执行。
4. `OperationSpec` 只表达原子动作。
5. `WorkflowStepSpec` 只编排单步 observe/filter/anchor/operation。
