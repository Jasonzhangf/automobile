# Blocks Interface Spec v1

## 目标

定义 Flowy 的最小能力块接口真源。

这里的 `block` 指：

- 不直接承载长流程
- 只完成一个能力闭环
- 可被 `flows` 复用
- 可被 CLI / WebSocket / runtime 统一驱动

第一版先固定 5 个核心 block：

1. `observe-page`
2. `filter-targets`
3. `evaluate-anchor`
4. `execute-operation`
5. `emit-event`

---

## 一、block 的定位

`foundation / blocks / flows` 三层里：

- `foundation`：纯函数、helper、matcher
- `blocks`：最小能力闭环
- `flows`：多 block 编排

因此 block 的边界是：

- 可以调用 foundation
- 不能复制 foundation
- 不能承载长流程状态机
- 不能私自变成 flow

---

## 二、统一接口原则

第一版所有 block 都遵守统一形状：

```json
{
  "schemaVersion": "flowy-block-spec-v1",
  "blockId": "observe_page_default",
  "kind": "observe-page",
  "input": {},
  "timeoutMs": 5000
}
```

执行结果统一形状：

```json
{
  "status": "ok",
  "startedAt": "2026-04-22T18:00:00+08:00",
  "finishedAt": "2026-04-22T18:00:01+08:00",
  "durationMs": 1000,
  "output": {},
  "artifacts": [],
  "error": null
}
```

这样后面：

- flow runner 好接
- CLI 好调用
- WS 好传输
- regression fixtures 好固化

---

## 三、5 个核心 block

### 1. `observe-page`

职责：

- 获取当前页面观察结果
- 输出统一 `PageState`

输入建议：

```json
{
  "observerSpec": {
    "requireAccessibility": true,
    "requireScreenshot": false
  }
}
```

输出建议：

```json
{
  "pageStateRef": "page-state:current",
  "pageSignature": "e0644857b0d8"
}
```

规则：

- 不直接做 filter
- 不直接做 operation

---

### 2. `filter-targets`

职责：

- 从 `PageState` 提取当前关心目标

输入建议：

```json
{
  "pageStateRef": "page-state:current",
  "filterRef": "filter:search_entry_filter"
}
```

输出建议：

```json
{
  "targetCount": 1,
  "selectedTargetRef": "target:search_button",
  "targetRefs": ["target:search_button"]
}
```

规则：

- 只做筛选
- 不负责 anchor
- 不负责点击

---

### 3. `evaluate-anchor`

职责：

- 判断 pre-anchor 或 post-anchor 是否满足

输入建议：

```json
{
  "pageStateRef": "page-state:current",
  "anchorRef": "anchor:open_xhs_search_anchor",
  "phase": "pre"
}
```

输出建议：

```json
{
  "matched": true,
  "phase": "pre",
  "reasons": ["mustContainTexts satisfied", "target visible"]
}
```

规则：

- 只判断状态
- 不触发重试
- 不做操作

---

### 4. `execute-operation`

职责：

- 执行一个原子操作

输入建议：

```json
{
  "operationRef": "operation:tap_search_entry",
  "targetRef": "target:search_button"
}
```

输出建议：

```json
{
  "operationId": "tap_search_entry",
  "status": "ok"
}
```

规则：

- 只执行 operation
- 不验证 post-anchor

---

### 5. `emit-event`

职责：

- 发送统一事件到 control daemon / CLI / WS 消费层

输入建议：

```json
{
  "eventType": "workflow.step.succeeded",
  "requestId": "req_20260422_0001",
  "runId": "run_20260422_0001",
  "data": {
    "pageSignature": "e0644857b0d8"
  }
}
```

输出建议：

```json
{
  "accepted": true
}
```

规则：

- 只发事件
- 不修改 workflow 状态

---

## 四、block 间串联方式

单步 flow 固定按下面顺序编排：

```text
observe-page
-> filter-targets
-> evaluate-anchor(pre)
-> execute-operation
-> observe-page
-> evaluate-anchor(post)
-> emit-event
```

block 本身不能偷偷跨层：

- `observe-page` 不能直接发成功事件
- `execute-operation` 不能自己判断 anchor
- `filter-targets` 不能自己触发点击

---

## 五、输入输出引用规则

block 之间统一用 `ref` 串联：

- `page-state:current`
- `target:search_button`
- `filter:search_entry_filter`
- `anchor:open_xhs_search_anchor`
- `operation:tap_search_entry`

这样：

- flow 层只编排 ref
- runtime 层只解析 ref

---

## 六、错误模型

每个 block 都统一返回错误对象：

```json
{
  "code": "target_not_found",
  "message": "filter returned zero targets",
  "retryable": false
}
```

规则：

- 禁止静默失败
- block 不吞错误
- flow 决定是否重试

---

## 七、第一批 examples

第一批固定样本：

1. `observe-page.example.json`
2. `filter-targets.search-entry.example.json`
3. `evaluate-anchor.pre-open-search.example.json`
4. `execute-operation.tap-search.example.json`
5. `emit-event.workflow-succeeded.example.json`

---

## 八、当前接受的设计结论

1. block 是最小能力闭环，不是长流程。
2. block 统一输入输出形状，方便 CLI / WS / flow / runtime 共用。
3. block 之间通过 ref 串联。
4. `observe / filter / anchor / operation / event` 是第一批固定 block。
