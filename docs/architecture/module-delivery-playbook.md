# Module Delivery Playbook v1

## 目标

把 Flowy 的“开发 -> 验证 -> 沉淀”固定成一套可重复执行的标准动作。

这份文档回答 4 个问题：

1. 一个模块从哪里开始
2. block 怎么才算可交付
3. build/test/evidence 怎样串起来
4. 结论应该沉淀到哪里

---

## 一、适用范围

适用于：

- `packages/foundation/`
- `packages/blocks/`
- `packages/flows/`
- `services/`
- `apps/`
- `explore/`

其中当前优先级最高的是：

```text
foundation -> blocks -> flows
```

---

## 二、标准开发节奏

每次开发都固定走下面的节奏：

```text
scope
-> truth
-> fixtures
-> tests
-> implementation
-> verify
-> evidence
-> distill
```

---

## 三、8 步交付动作

### 1. scope：先缩小任务边界

先明确：

- 这个改动属于哪一层
- 最小交付是什么
- 什么证据能证明它真的通了

如果最小证据说不清，不进入实现。

---

### 2. truth：先改真源，再写实现

优先修改：

1. `docs/architecture/*.md`
2. `packages/*` 中的 schema / example / interface
3. `AGENTS.md`（仅稳定事实）

禁止直接跳过真源、先写散代码。

---

### 3. fixtures：先准备样本

每个模块都要先定义最小样本。

block 至少要有：

- 1 个标准 input example
- 1 个 success result example
- 1 个 error result example

如果是边界敏感模块，还要补：

- boundary example
- invalid example
- retryable / non-retryable error example

没有样本，测试容易漂。

---

### 4. tests：先写门禁，再写实现

当前标准顺序：

#### foundation
- 单测

#### blocks
- 标准单测
- 覆盖测试
- result / error shape 测试
- 边界样本测试

#### flows
- flow 编排测试
- regression replay
- real smoke

block 没过自己的测试门禁，不进入 flow。

---

### 5. implementation：只补最小闭环

实现时必须遵守：

- 只改当前层应该承担的内容
- 相同能力只保留一个真源
- 不把 block 逻辑偷偷塞进 flow
- 不把平台细节写死进共享层

目标不是“一次写完”，而是“最小可验证”。

---

### 6. verify：固定门禁顺序

固定顺序（五级递进，缺一不可）：

```text
check-file-lines                    # L0: 文件门禁
-> unit tests                       # L1: 单元测试（纯函数 / 最小模块）
-> block coverage tests             # L2: 覆盖测试（每个 block 的正常 + 错误 + 边界）
-> flow orchestration tests         # L3: 编排测试（状态机串联、mock 环境）
-> real-device E2E test             # L4: 真机端到端（真实设备 + 真实 APP + 完整流程）
-> build                            # 最后才是 build
```

**硬规则：没有真机端到端验证，不得宣称完成。**

五级测试定义：

| 级别 | 名称 | 环境 | 通过标准 |
|------|------|------|----------|
| L1 | 单元测试 | 本地 Go/JVM | 每个函数正常路径 + 至少一个错误路径 |
| L2 | 覆盖测试 | 本地 Go/JVM | 每个 block 有 success/error/boundary 覆盖 |
| L3 | 编排测试 | 本地 Go/JVM + mock | flow 状态机串联跑通 happy path |
| L4 | 真机 E2E | **真实 Android 设备** | 从入口到结果的完整闭环，有截图/日志 artifact |

L4 必须提供以下证据之一：
- 截图（artifact 路径）
- 完整 response.json / collection-result.json
- note.md 中记录的观察证据

任何一级失败，都不能宣称完成，不能 close bd task。

---

### 7. evidence：输出可复核证据

每次交付都至少给出：

1. 改了哪些文件
2. 跑了哪些命令
3. 命令结果是什么
4. 还有什么没验证

如果是运行时能力，还要补：

- log
- screenshot
- response.json
- artifact path

---

### 8. distill：最后沉淀

沉淀规则固定如下：

- `note.md`：探索过程、失败原因、临时猜测
- `MEMORY.md`：能跨轮次复用的稳定经验
- `AGENTS.md`：已经接受的项目事实和硬规则
- `.agent/flowy-dev-skill/SKILL.md`：未来开发时应重复执行的流程

不要把同一段流程同时抄到 4 个地方。

---

## 四、block 交付专用门禁

每个 block 都必须按下面顺序达标：

### Gate 1：接口真源

- block input shape 已定义
- block result shape 已定义
- error shape 已定义

### Gate 2：标准样本

- input example 存在
- success result example 存在
- error result example 存在

### Gate 3：标准测试

- 单测通过
- result / error shape 测试通过

### Gate 4：覆盖测试

- 每个 core block 都有样本覆盖
- 每个 core block 都有 success / error 覆盖
- 关键边界输入已覆盖

### Gate 5：编排前验证

确认 flow 可以安全依赖这个 block：

- ref 命名稳定
- 错误语义稳定
- timeout / retry 约定稳定

通过前 5 门，才允许把这个 block 接入 flow。

---

## 五、build 入口必须可执行这些规则

流程不能只停留在文档里。

至少要做到：

- `scripts/verify/` 里有可直接执行的门禁脚本
- `scripts/dev/` 的 build 入口真实调用这些脚本
- build 失败时能直接指出是哪一门失败

对于当前 Android lab：

```text
scripts/dev/build-android-lab.sh
```

必须把下面这些 gate 串起来：

1. `scripts/verify/check-file-lines.sh`
2. `scripts/verify/blocks-spec-unit.sh`
3. `scripts/verify/blocks-spec-coverage.sh`
4. Gradle build / APK artifact check

---

## 六、什么时候才能推进到 flow

只有在下面条件同时满足后，才能继续 flow：

1. foundation 真源清楚
2. block 接口真源清楚
3. block 单测通过
4. block 覆盖测试通过
5. build 入口已纳入这些 gate

否则继续补 block，不提前进入 flow。

---

## 七、交付汇报模板

固定用下面结构：

### 1. 变更
- 文档
- 脚本
- 测试

### 2. 证据
- 执行命令
- 结果摘要
- artifact 路径

### 3. 未验证项
- 还缺什么真实证据

### 4. 下一步
- 只写一个最小下一步

这样可以避免“说了很多，但没有可复核闭环”。
