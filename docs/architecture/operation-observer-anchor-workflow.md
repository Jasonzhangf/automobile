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

补充要求：

- `post-anchor` 不能默认做单次判定。
- 对跨页面 / 跨 APP 跳转，运行时必须允许 **bounded retry / poll**：
  - 重复 `observe`
  - 直到拿到新的 page state / 新的 accessibility snapshot
  - 或达到 timeout / max-attempts 再失败
- 否则会把“操作已成功、观察还没刷新”误判成 workflow 失败。

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

围绕刚才已经真机打通的最小闭环，基座先固定成下面三层：

```text
foundation
-> leaf blocks
-> skeleton flows
```

### 1. foundation：纯工具 / 共享状态 / 真值构造

这一层不直接对外暴露“业务动作”，只负责把底层真值和通用工具准备好。

第一批固定模块：

- `DisplayInfoReader`
  - 读取屏幕宽高、dpi、rotation
- `PageContextBuilder`
  - 统一产出 `page-context.json`
- `AccessibilitySnapshotStore`
  - 持有最近一次前台 Accessibility 快照
- `MediaProjectionSessionHolder`
  - 持有截图授权与长驻 projection session
- `AccessibilityTargeting`
  - 目标筛选、bounds 计算、点击中心点推导
- `VersionReader / DevServerReader / UpgradeStateStore`
  - 版本、daemon 地址、升级状态真源

规则：

- foundation 可以被 blocks 复用
- foundation 不直接发 WS 命令结果
- foundation 不直接推进 workflow

### 2. leaf blocks：真实原子能力

这一层就是“手机上已经确认能做”的最小能力块。

当前第一批叶子 block 固定为：

#### Observe 类
- `CaptureScreenshotBlock`
- `DumpAccessibilityTreeBlock`
- `FetchLogsBlock`

#### Operate 类
- `TapBlock`
- `ScrollBlock`
- `InputTextBlock`
- `PressKeyBlock`
- `BackBlock`

#### Runtime / Transport 类
- `UploadArtifactBlock`
- `AppendLogBlock`
- `CheckUpgradeBlock`
- `DownloadUpgradeApkBlock`
- `PromptInstallApkBlock`

规则：

- leaf block 只完成一个能力闭环
- 输入输出保持稳定 result 形状
- 一个 block 只对应一个真能力，不混多个动作

### 3. skeleton flows：基座骨架编排

这一层不新增底层能力，只编排 leaf blocks。

第一批 skeleton flow：

- `DaemonStartupFlow`
  - 前台 service / ws 连接 / hello / reconnect
- `WsSessionFlow`
  - 命令收发与协议分发
- `OperationRunFlow`
  - `tap / scroll / input-text / press-key / back` 统一执行入口
- `ScreenshotCaptureFlow`
  - `capture-screenshot -> upload artifacts -> response`
- `AccessibilityDumpFlow`
  - `dump-accessibility-tree -> upload artifacts -> response`
- `UpgradeCheckFlow`
  - 检查升级 / 下载安装 / 安装提示

规则：

- flow 不复制 block 能力
- flow 只做“拿哪个 block + 何时调用 + 如何组 response/event”
- flow 是后续 workflow engine 的现阶段过渡层

### 4. 下一步要从 leaf blocks 升到 workflow skeleton 的能力

基于当前已经验证的能力，后续正式 workflow 基座固定抽象成：

```text
observe-page
  -> capture-screenshot / dump-accessibility-tree / page-context
filter-targets
  -> clickable / scrollable / input / image / card
evaluate-anchor
  -> pre / post constraints
execute-operation
  -> tap / scroll / input / press-key / back
emit-event
  -> page.observed / operation.finished / workflow.step.*
```

也就是说：

- **当前已有的真实 block 不推倒重来**
- 而是把它们作为 workflow skeleton 的叶子执行单元
- 这样后面接小红书/微博/系统设置时，只换 filter / anchor / field mapping，不换底层能力

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
