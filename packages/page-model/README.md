# packages/page-model

共享页面执行模型真源 (Go daemon + Kotlin Android daemon + 任何 client)。

## 文件

- `filter-spec.schema.json` — FilterSpec (节点选择条件)
- `anchor-spec.schema.json` — AnchorSpec (页面锚点条件)
- `operation-backend.schema.json` — OperationBackend (操作 backend 选择)
- `examples/*.json` — 示例 payload

## 字段对齐 (与 Go + Kotlin 实现对比)

### FilterSpec

| 字段 | Go | Kotlin | Status |
|------|----|----|-------|
| className | ✓ | classNameContains | ✗ 不一致 (Kotlin 改 contains) |
| textContains | ✓ | textContains | ✓ |
| textMatches | ✓ | - | ✗ Kotlin 缺 |
| descContains | ✓ | contentDescContains | ✗ 不一致 (字段名) |
| descMatches | ✓ | - | ✗ Kotlin 缺 |
| notDesc | ✓ | - | ✗ Kotlin 缺 |
| minTextLength/maxTextLength | ✓ | - | ✗ Kotlin 缺 |
| clickable | ✓ | clickable | ✓ |
| editable | ✓ | editable | ✓ |
| scrollable | ✓ | scrollable | ✓ |
| selected | ✓ | - | ✗ Kotlin 缺 |
| hasBounds | ✓ | bounds | ✗ 不一致 (字段名) |
| longClickable | - | longClickable | ✗ Kotlin 多 |
| selectStrategy | ✓ | - | ✗ Kotlin 缺 |

### AnchorSpec

| 字段 | Go | Kotlin | Status |
|------|----|----|-------|
| mustContainTexts | ✓ | checkTexts | ✓ (实现) |
| mustNotContainTexts | ✓ | - | ✗ Kotlin 缺 |
| targetVisible/targetDescContains | ✓ | - | ✗ Kotlin 缺 |
| minNodeCount/maxNodeCount | ✓ | - | ✗ Kotlin 缺 |
| packageName | ✓ | checkPackageName | ✓ |
| pageSignature | - | checkPageSignature | ✗ Kotlin 独有 |
| projectionReady | - | checkProjectionReady | ✗ Kotlin 独有 |

### OperationBackend

| 值 | Go | Kotlin |
|----|----|-------|
| accessibility | "accessibility" | ACCESSIBILITY |
| root | "root" | ROOT |

字段不一致。需要建立 go-side 和 kotlin-side 的双向映射。
