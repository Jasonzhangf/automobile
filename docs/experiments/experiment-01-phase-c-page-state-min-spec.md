# Experiment 01 / Phase C — PageState 最小归一化规格

## 目标

Phase C 不增加新的设备权限，也不增加新的操作能力。

它只做一件事：

```text
把 Phase B 已经拿到的 accessibility-raw.json
归一化成一个可读、可比对、可生成命令的最小 PageState
```

Phase C 要证明的不是“自动化已经会操作页面”，而是先证明：

1. 同一份 raw tree 可以稳定提炼出页面标题、页面锚点、可操作元素
2. 这些信息足以支持人类 review
3. 这些信息足以作为下一阶段 command model 的输入

如果这层都不稳定，就不应该继续做页面命令化和流程编排。

## 当前边界

### Phase C 要做
- 定义 `PageState` 的最小 JSON 结构
- 定义从 `accessibility-raw.json` 到 `PageState` 的最小归一化规则
- 做一个 **Mac 本地离线提取工具**
- 用真实 artifact 跑出第一个 `page-state.json`

### Phase C 不做
- Android 端新增 `capture-page` 命令
- Mac daemon 新增 page-state upload
- OCR / vision 融合
- 操作执行
- 命令规划器
- 正式 `packages/page-model` schema 工程

## 真源边界

当前 `PageState` 还是实验输出，不属于 exp01 传输协议真源。

因此：

- 传输协议继续以 `docs/experiments/exp01-json-truth-spec.md` 为唯一真源
- `PageState` 的实验规则以本文件为真源
- 只有当 Phase C 证明稳定后，才允许迁移到：
  - `packages/page-model/`
  - 或正式 daemon command / artifact 协议

## 输入

Phase C 当前只依赖两类输入：

1. `accessibility-raw.json`
2. 可选的 `page-context.json`

其中：

- `accessibility-raw.json` 是结构真源
- `page-context.json` 只做补充上下文，不反向覆盖 raw tree 结构事实

## 输出：page_state_json

### JSON
```json
{
  "schemaVersion": "exp01-page-state-v1",
  "generatedAt": "2026-04-22T12:45:00+08:00",
  "source": {
    "kind": "accessibility-raw",
    "requestId": "req_20260422_123631_0008",
    "runId": "2026-04-22T12-36-31_dump-accessibility-tree_settings-tree",
    "capturedAt": "2026-04-22T12:36:31+08:00"
  },
  "app": {
    "packageName": "com.android.settings",
    "windowTitle": null
  },
  "screen": {
    "widthPx": 1140,
    "heightPx": 2616,
    "rotation": 0,
    "densityDpi": 520
  },
  "page": {
    "title": "无线调试",
    "signatureSeed": "com.android.settings|无线调试|返回|设备名称|IP 地址和端口|使用二维码配对设备",
    "signature": "770b0c1e4d3b",
    "anchorTexts": [
      "无线调试",
      "设备名称",
      "IP 地址和端口",
      "使用二维码配对设备"
    ],
    "stats": {
      "nodeCount": 40,
      "anchorCount": 6,
      "elementCount": 8,
      "actionableCount": 7,
      "scrollableCount": 1
    }
  },
  "anchors": [],
  "elements": []
}
```

## 顶层字段要求

### 1. `schemaVersion`
- 当前固定：`exp01-page-state-v1`

### 2. `generatedAt`
- PageState 在 Mac 侧生成的时间

### 3. `source`
- 标记来源 artifact
- 当前 `kind` 固定为 `accessibility-raw`

### 4. `app`
- `packageName` 优先来自 raw tree
- `windowTitle` 可由 `page-context.json` 补充

### 5. `screen`
- 当前优先来自 `page-context.json`
- 如果没有 screen 信息，允许写 `null`

### 6. `page`
- `title`：页面主标题，最小归一化结果
- `signatureSeed`：可读的签名原始串，用于调试
- `signature`：对 `signatureSeed` 做稳定 hash 后的短签名
- `anchorTexts`：最重要的页面锚点文本
- `stats`：帮助人工快速判断抽取质量

## anchors 规则

`anchors` 表示 **帮助识别当前页面身份** 的非空文本锚点。

每个 anchor 最少包含：

```json
{
  "nodeId": "n3",
  "text": "无线调试",
  "kind": "title",
  "boundsInScreen": {
    "left": 143,
    "top": 120,
    "right": 406,
    "bottom": 199
  }
}
```

### `kind` 当前允许值
- `title`
- `section`
- `text`

### 选择规则
- 只从 `visibleToUser=true` 的节点中选
- 只选非空文本
- 标题优先：
  - 上半屏
  - 文本较短
  - 更像页面主标题
- section 其次：
  - 重要分组标题
  - 列表标题

## elements 规则

`elements` 表示 **下一阶段最可能变成命令目标** 的节点。

当前只收这几类：

- clickable
- editable
- checkable
- scrollable
- longClickable
- 或者类名本身明显属于控件角色

### 每个 element 的最小字段
```json
{
  "elementId": "view:coui_toolbar_back_view",
  "nodeId": "n2",
  "role": "button",
  "label": "返回",
  "value": null,
  "hint": null,
  "actions": ["click"],
  "boundsInScreen": {
    "left": 46,
    "top": 108,
    "right": 134,
    "bottom": 196
  },
  "locator": {
    "viewIdResourceName": "com.android.settings:id/coui_toolbar_back_view",
    "className": "android.widget.ImageButton",
    "parentNodeId": "n1"
  }
}
```

### role 最小映射
- `ImageButton` / `Button` -> `button`
- `EditText` -> `input`
- `Switch` / `SwitchCompat` -> `switch`
- `CheckBox` / `RadioButton` -> `check`
- `RecyclerView` / `ListView` / `ScrollView` -> `scroll_container`
- 其他但 clickable -> `action`

### label 规则
- 优先使用当前节点自己的：
  - `text`
  - `contentDescription`
  - `hintText`
  - `paneTitle`
- 如果当前节点没有文字，但它是 actionable container：
  - 可以聚合可见子节点文本

### value 规则
- 当前只做最小提取
- 若聚合文本里出现主标题 + 次级说明：
  - 第一段作为 `label`
  - 其余作为 `value`

### elementId 规则
当前 `elementId` 是 **best-effort 稳定键**，不是永久真主键。

优先顺序：

1. `view:<viewIdResourceName 最后一个 segment>`
2. `role:<role>|label:<slug(label)>`
3. `role:<role>|node:<nodeId>` 仅用于 debug fallback

## 归一化硬规则

### 1. 只做可解释提取
- 不能生成模型自己编的字段
- 所有字段都必须能回溯到 raw tree

### 2. 不吞冲突
- 如果标题不明确，`page.title` 可以为 `null`
- 不允许为了“像成功”而随便猜一个标题

### 3. 先 inspectability，再 automation
- 当前 PageState 先服务人工 review
- 元素抽取可少，不可乱

### 4. 保留调试路径
- `signatureSeed` 必须保留，便于回看为什么这个页面签名变了

## 最小验证标准

用真实 artifact 跑完后，人工必须能回答：

1. 当前页面标题是什么
2. 当前页面属于哪个 package
3. 当前页面有哪些主要锚点文本
4. 当前页面有哪些候选可操作元素
5. 当前页面有没有 scroll container

如果这五个问题答不出来，就说明 PageState 还不能进入 command model 阶段。
