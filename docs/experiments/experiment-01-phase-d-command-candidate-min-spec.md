# Experiment 01 / Phase D — Command Candidate 最小规格

## 目标

Phase D 不直接执行操作。

它只做这一步：

```text
PageState
-> 提炼出当前页面最合理的一组候选命令
-> 让人类或上层 agent 选择
```

Phase D 要证明的是：

1. 当前页面里哪些元素更像“可操作目标”
2. 每个目标最适合哪种命令类型
3. 这些命令是否足够稳定，能进入后续流程编排

如果这层还不稳定，就不应该继续做自动执行。

## 当前边界

### 要做
- 定义 `command-candidates.json` 最小结构
- 定义从 `page-state.json` 到候选命令的最小生成规则
- 提供一个 Mac 本地离线生成脚本
- 用真实 `page-state.json` 做 smoke

### 不做
- Android 端执行命令
- 命令回放
- 坐标点击
- gesture synthesis
- LLM 规划器
- 多步 flow engine

## 输入

当前只接受：

1. `page-state.json`

它必须已经包含：

- 页面标题
- 候选元素
- 元素的 role / label / actions / locator

## 输出：command_candidates_json

### JSON
```json
{
  "schemaVersion": "exp01-command-candidates-v1",
  "generatedAt": "2026-04-22T13:05:00+08:00",
  "source": {
    "kind": "page-state",
    "runId": "2026-04-22T12-36-31_dump-accessibility-tree_settings-tree",
    "pageSignature": "b40efef1dff7"
  },
  "page": {
    "packageName": "com.android.settings",
    "title": "无线调试",
    "signature": "b40efef1dff7"
  },
  "stats": {
    "elementCount": 12,
    "commandCount": 14
  },
  "commands": [
    {
      "commandId": "tap:view:coui_toolbar_back_view",
      "kind": "tap",
      "target": {
        "elementId": "view:coui_toolbar_back_view",
        "role": "button",
        "label": "返回"
      },
      "args": {},
      "confidence": "high",
      "priority": 100,
      "rationale": [
        "element exposes click action",
        "role=button",
        "label present"
      ]
    }
  ]
}
```

## 顶层字段要求

### 1. `schemaVersion`
- 当前固定：`exp01-command-candidates-v1`

### 2. `source`
- `kind` 当前固定 `page-state`
- `runId` / `pageSignature` 用于回溯

### 3. `page`
- 当前页面身份摘要

### 4. `stats`
- 便于人工快速判断候选质量

### 5. `commands`
- 以优先级排序
- 当前只放“单步候选”

## command 对象最小字段

### 1. `commandId`
- 当前 best-effort 稳定 id
- 规则：
  - `tap:<elementId>`
  - `long-press:<elementId>`
  - `scroll-forward:<elementId>`
  - `scroll-backward:<elementId>`
  - `input-text:<elementId>`
  - `toggle:<elementId>`

### 2. `kind`
当前允许值：

- `tap`
- `long-press`
- `scroll`
- `input-text`
- `toggle`

### 3. `target`
最少包含：

```json
{
  "elementId": "view:coui_toolbar_back_view",
  "role": "button",
  "label": "返回"
}
```

### 4. `args`
- `tap` / `long-press` 可为空对象
- `scroll` 必须带：
  - `direction: forward | backward`
- `input-text` 必须带：
  - `text: "<TEXT>"`
- `toggle` 必须带：
  - `mode: toggle`

### 5. `confidence`
当前允许值：

- `high`
- `medium`
- `low`

### 6. `priority`
- 越大越靠前
- 当前只是启发式排序，不是最终规划真值

### 7. `rationale`
- 必须能解释“为什么产出这个候选”
- 不允许黑箱结论

## 生成规则

### 1. click -> tap
若元素包含 `click`：
- 生成一个 `tap`

### 2. long-click -> long-press
若元素包含 `long-click`：
- 生成一个 `long-press`

### 3. scroll -> 双向 scroll
若元素包含 `scroll`：
- 生成两个候选：
  - `scroll forward`
  - `scroll backward`

### 4. input -> input-text
若元素包含 `input`：
- 生成一个模板命令：
  - `text: "<TEXT>"`

### 5. toggle -> toggle
若元素包含 `toggle`：
- 生成一个 `toggle`

## 优先级启发式

### 高优先级
- 明确 button / input / switch / scroll_container
- 有清晰 label
- 有 viewId 或稳定 elementId

### 中优先级
- role = action
- 有 label
- 动作明确

### 低优先级
- label 很弱
- 容器性太强
- 信息不足但仍保留作 debug

## 去重规则

当前候选必须按 `commandId` 去重：

- 同一 `commandId` 多次出现时只保留一份
- 保留优先级更高的一份

## 最小验证标准

基于真实页面，人工必须能回答：

1. 返回按钮对应哪个命令
2. 主列表对应哪个 scroll 命令
3. 主要条目是否都有 tap 候选
4. 候选命令是否能解释为什么被生成

如果这四个问题答不出来，说明 command candidate 还不能进入执行层。
