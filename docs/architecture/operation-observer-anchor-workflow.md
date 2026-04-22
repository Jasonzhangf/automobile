# Operation Observer Anchor Workflow v0.1

## 目标

把 Flowy 的正式基座收敛成一个统一执行模型：

```text
observe current page
-> filter target
-> build anchor constraints
-> execute operation
-> observe result page
-> verify anchor transition
-> emit workflow event
```

这套模型不再围绕“单次实验”组织，而是围绕 **可复用运行时基座** 组织。

---

## 一、核心抽象

### 1. Operation

`Operation` 表示手机端“能做什么”。

它只描述 **执行能力**，不负责理解页面。

建议最小集合：

- `tap`
- `long_press`
- `scroll`
- `input_text`
- `back`
- `home`
- `wait`
- `capture_screenshot`

规则：

- Operation 必须是 **原子能力**。
- Operation 输入必须可序列化。
- Operation 执行结果必须返回：
  - `status`
  - `startedAt`
  - `finishedAt`
  - `durationMs`
  - `error`
  - `artifacts`

建议结构：

```json
{
  "operationId": "tap_card_001",
  "kind": "tap",
  "target": {
    "bounds": { "left": 16, "top": 250, "right": 562, "bottom": 1087 }
  },
  "args": {},
  "timeoutMs": 3000
}
```

---

### 2. Observer

`Observer` 表示页面观测器。

职责：

1. 获取当前页面原始观测数据
2. 归一化为 `PageState`
3. 根据 `filter` 提取当前流程真正关心的目标

Observer 不是一个单实现，而是一个分层管线：

```text
raw accessibility
-> optional screenshot
-> normalized PageState
-> filtered targets
```

建议分 3 层：

#### A. Raw Observer
- dump accessibility tree
- capture screenshot
- collect page-context

#### B. Page Observer
- 输出统一 `PageState`
- 统一页面签名 `pageSignature`
- 提取 `anchors / elements / stats`

#### C. Filter Observer
- 按 workflow 关注点提取结果
- 只返回当前流程需要的目标

例如：

- `settings_toggle_filter`
- `feed_card_filter`
- `search_input_filter`
- `blocker_filter`

---

### 3. Anchor

`Anchor` 是状态机稳定性的关键。

它不是一个“点”，而是一组 **点击前后必须满足的限制条件**。

作用：

1. 保证点击前定位的是对的目标
2. 保证点击后到达的是期待状态
3. 保证 workflow 不因页面抖动或内容变化跑偏

Anchor 必须支持两个方向：

#### A. Pre-anchor
执行前验证当前目标是否可信。

例子：

- 页面签名匹配
- 目标文本存在
- 目标 bounds 仍在可见区域
- 目标所在卡片仍包含作者/标题/按钮等锚点

#### B. Post-anchor
执行后验证页面是否进入正确结果态。

例子：

- 新页面标题出现
- 某个输入框获得焦点
- 某个 toast / tab / action bar 出现
- 某个 blocker 消失

建议结构：

```json
{
  "preAnchor": {
    "pageSignature": "5d9ad4460566",
    "mustContainTexts": ["搜索", "发现"],
    "targetBoundsStable": true
  },
  "postAnchor": {
    "anyOf": [
      { "pageTitle": "问ai" },
      { "mustContainTexts": ["猜你想搜", "历史记录"] }
    ]
  }
}
```

---

### 4. Workflow

`Workflow` 是完整任务流程。

它不是直接点坐标，而是：

```text
observe
-> decide
-> verify pre-anchor
-> operate
-> observe
-> verify post-anchor
-> continue / retry / fail
```

最小单步 workflow 由 4 部分组成：

1. `observerSpec`
2. `targetFilter`
3. `operationPlan`
4. `anchorPolicy`

建议结构：

```json
{
  "workflowId": "open_xhs_search",
  "observerSpec": {
    "requireAccessibility": true,
    "requireScreenshot": false,
    "filters": ["search_entry_filter"]
  },
  "operationPlan": {
    "kind": "tap",
    "targetRef": "target:search_button"
  },
  "anchorPolicy": {
    "preAnchor": { "mustContainTexts": ["搜索", "发现"] },
    "postAnchor": { "mustContainTexts": ["问ai", "猜你想搜"] }
  }
}
```

---

## 二、为什么要用 operation + event

你要求的主架构是 `operation + event`，这是对的。

推荐把运行时拆成：

### 1. Command / Workflow 输入层
- control daemon / CLI / agent 下发 workflow / step

### 2. Event 总线
- 所有观测、执行、校验都发事件

### 3. Operation Executor
- 只负责执行操作

### 4. Observer Pipeline
- 只负责产出页面事实

### 5. Anchor Evaluator
- 只负责判断状态是否成立

### 6. Workflow Engine
- 只负责状态机推进

事件建议最小集合：

- `page.observed`
- `filter.matched`
- `anchor.pre.checked`
- `operation.started`
- `operation.finished`
- `anchor.post.checked`
- `workflow.step.succeeded`
- `workflow.step.failed`
- `workflow.completed`

补充要求：

- CLI 与 WebSocket 不是两套执行模型
- 它们都只是在同一个 `Command / Workflow 输入层` 进入
- 后面统一进入：
  - `Observer Pipeline`
  - `Anchor Evaluator`
  - `Operation Executor`
  - `Workflow Engine`

---

## 三、模块边界

按你要求，正式基座建议拆成 4 个主模块。

### 1. `operation/`

职责：执行原子动作。

建议子域：

- `gesture/`
- `input/`
- `system/`
- `capture/`

典型接口：

```text
execute(OperationSpec) -> OperationResult
```

---

### 2. `observer/`

职责：获取页面事实。

建议子域：

- `raw-accessibility/`
- `raw-screenshot/`
- `page-state/`
- `filters/`

典型接口：

```text
observe(ObserverSpec) -> ObservationBundle
applyFilter(PageState, FilterSpec) -> FilterResult
```

---

### 3. `anchor/`

职责：校验状态机条件。

建议子域：

- `page-anchor/`
- `target-anchor/`
- `transition-anchor/`

典型接口：

```text
evaluatePreAnchor(PageState, Target, AnchorSpec) -> AnchorCheckResult
evaluatePostAnchor(PageState, AnchorSpec) -> AnchorCheckResult
```

---

### 4. `workflow/`

职责：编排 observe / anchor / operate / verify。

建议子域：

- `step-runner/`
- `retry-policy/`
- `failure-policy/`
- `event-emitter/`

典型接口：

```text
runStep(WorkflowStepSpec) -> StepResult
runWorkflow(WorkflowSpec) -> WorkflowResult
```

---

## 四、推荐执行顺序

正式实现时，不要一上来做大而全 workflow。

按这个顺序：

### Phase 1：Operation 基座
- 定义 operation schema
- 跑通 tap / scroll / input / back / wait

### Phase 2：Observer 基座
- 固化 `PageState`
- 固化 filter interface
- 先支持：
  - `actionable`
  - `scrollable`
  - `input`
  - `card-like`
  - `blocker`

### Phase 3：Anchor 基座
- 先做 page anchor
- 再做 target anchor
- 最后做 transition anchor

### Phase 4：Workflow Engine
- 先做单步 workflow
- 再做多步 workflow
- 最后加 retry / rollback-like checkpoint（不是回滚代码，而是回退流程状态）

---

## 五、当前接受的设计结论

1. Flowy 的正式基座采用 **operation + event** 架构。
2. 页面执行闭环固定为：
   - `observe -> filter -> pre-anchor -> operation -> observe -> post-anchor`
3. `Observer` 必须先产出统一 `PageState`，再做 filter。
4. `Anchor` 是 workflow 稳定性的必要模块，不是可选优化。
5. `feed/card` 页必须区分：
   - 语义目标
   - 执行目标
6. 当系统不给稳定 click target 时，允许基于 card bounds 合成 tap target。
7. `Accessibility` 仍是第一真源；`screenshot/vision` 用于补洞；`OCR` 最后介入。
