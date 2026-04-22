# Development Workflow v1

## 目标

定义 Flowy 后续正式开发的固定流程：

1. 如何开一个模块
2. 如何做完整开发
3. 如何测试
4. 如何沉淀经验

---

## 一、一个模块的标准开发顺序

固定顺序：

```text
review scope
-> decide module boundary
-> write/update architecture truth
-> implement foundation
-> implement blocks
-> implement flows
-> verify
-> collect evidence
-> distill memory
```

---

## 二、模块开发前先回答 8 个问题

1. 这个能力属于哪个目录？
   - `packages/`
   - `services/`
   - `explore/`
   - `apps/`

2. 它属于哪一层？
   - `foundation`
   - `blocks`
   - `flows`

3. 它的输入输出 schema 是什么？

4. 它的最小验证证据是什么？

5. 它的经验沉淀应该写到哪里？

6. 它是不是已经在别处实现过？
   - 如果是，必须复用或下沉，不能复制一份

7. 它是不是依赖 control daemon / upgrade / transport 配置？
   - 如果是，必须走 `packages/config/` 真源，不能写死平台地址

8. 它是不是同时影响 CLI 和 WebSocket 控制面？
   - 如果是，必须验证二者是否仍共享同一套 schema / route / event

---

## 三、模块开发最小闭环

### Step 1：先定真源

先更新对应真源文档，而不是直接开写大段代码。

优先级：

1. `docs/architecture/*.md`
2. `AGENTS.md`（仅稳定事实）
3. `packages/*` 中的 schema / 接口

---

### Step 2：先 foundation，再 blocks，再 flows

不要直接把流程写死在一个文件里。

并且必须遵守“唯一真源实现”：

- 相同 helper 不允许在多个模块各拷一份
- 相同 block 能力不允许在不同 flow 中重复实现
- flow 只编排，不私自重写 block 细节
- CLI 和 WebSocket 的 command/event 不能各写一套近似实现

#### foundation
- 纯函数
- helper
- parser
- matcher
- serializer

#### blocks
- observer block
- operation execute block
- anchor evaluate block
- event emit block

#### flows
- open search flow
- dismiss blocker flow
- tap and verify flow

---

### Step 3：每做完一层就验证

不要等整个大流程写完再测。

推荐：

- foundation：单测
- blocks：模块集成测试
- flows：fixtures + 真实 smoke

---

## 四、测试流程

固定测试链：

```text
file-line gate
-> static / compile
-> unit tests
-> module integration tests
-> regression fixtures replay
-> real smoke
```

---

### 1. 文件门禁

每个源码文件必须：

- `<= 500 行`

这是第一门，不通过就不继续。

---

### 2. 单元测试

适用于：

- foundation
- parser
- selector
- anchor matcher
- command builder

目标：

- 快速验证纯逻辑

---

### 3. 模块集成测试

适用于：

- observer pipeline
- operation executor
- anchor evaluator
- workflow step runner

目标：

- 验证模块内部协作

---

### 4. 回归样本测试

共享样本统一来自：

- `packages/regression-fixtures/`

样本类型：

- accessibility raw
- page-context
- screenshot meta
- page-state expected
- command candidates expected

目标：

- 避免 page model / filter / anchor / workflow schema 回归
- 避免 control daemon mode / upgrade source / protocol 配置回归
- 避免 CLI 与 WebSocket 控制面 schema 漂移

---

### 5. 真实 smoke

这一步必须有证据。

证据可以是：

- run artifact
- logs
- screenshot
- response.json

规则：

- 没有真实证据，不算完成

---

## 五、经验沉淀流程

### 1. `note.md`

记录本轮探索：

- 做了什么
- 失败了什么
- 为什么失败
- 当前猜测

它是时间线日志。

---

### 2. `MEMORY.md`

沉淀可复用经验：

- 什么时候该选哪种实现
- 哪些坑会反复出现
- 哪些判断已经被真实证据验证

它是复用知识库。

---

### 3. `CACHE.md`

维护当前工作现场：

- 当前目标
- 当前 blocker
- 当前下一步

它是短期上下文。

---

### 4. `AGENTS.md`

只提升稳定事实：

- 已接受架构决策
- 已固定流程边界
- 硬规则

不要把试错过程写进去。

---

### 5. `.agent/flowy-dev-skill/`

当某种开发方法已经多次复用时，提升成 skill：

- 固定开发流程
- 固定调试手法
- 固定验证顺序

---

## 六、一个模块完成时的交付清单

至少要有：

1. 代码
2. 对应测试
3. 验证证据
4. 文档更新
5. 经验沉淀更新

展开为：

- 代码已落到正确目录
- 测试可从 `scripts/verify/` 入口跑
- 有真实 artifact 或日志证据
- `docs/architecture/` 已同步
- 必要时更新 `MEMORY.md` / `CACHE.md` / `AGENTS.md`
- 如果涉及 daemon / upgrade / transport，必须明确验证配置真源是否仍唯一
- 如果涉及 CLI / WebSocket 控制面，必须明确验证二者仍复用同一套 command/event 真源

---

## 七、当前接受的开发流程结论

1. 先定结构，再写代码。
2. 先定 schema，再写实现。
3. 先 foundation，再 blocks，再 flows。
4. 每次构建都必须带测试，不接受 compile-only。
5. 经验沉淀采用四层：
   - `note.md`
   - `MEMORY.md`
   - `CACHE.md`
   - `AGENTS.md`
6. 任何新功能都先检查是否已有唯一真源实现；没有才新增。
