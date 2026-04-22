# Workspace Structure v1

## 目标

把当前仓库收敛成一个 **双端运行时 + 共享真源 + 集中脚本 + 明确文档分层** 的 workspace。

这个文档回答 4 个问题：

1. 顶层目录是什么
2. 每一层放什么
3. 新模块应该落在哪里
4. 哪些内容禁止乱放

---

## 一、顶层目录

```text
flowy/
├─ AGENTS.md
├─ MEMORY.md
├─ CACHE.md
├─ note.md
├─ .agent/
├─ docs/
├─ scripts/
├─ services/
├─ explore/
├─ apps/
├─ packages/
└─ artifacts/
```

---

## 二、顶层职责

### 1. `AGENTS.md`

只放：

- 项目事实
- 硬边界
- 已接受决策
- 文档路由

不放：

- 时间线日志
- 试错过程
- 长篇流程细节

---

### 2. `MEMORY.md`

放跨轮次仍然有价值的稳定经验：

- 已验证的架构判断
- 反复踩坑后的预防规则
- 适用于后续模块开发的复用经验

这是“经验沉淀”的主真源。

---

### 3. `CACHE.md`

放当前阶段的短期执行上下文：

- 正在推进的模块
- 当前 blocker
- 近期下一步

这是“当前工作现场”的真源，不是长期知识库。

---

### 4. `note.md`

放探索过程：

- 假设
- 实验记录
- 失败样本
- 临时判断

稳定结论不长期停留在这里，要提升到 `AGENTS.md` 或 `MEMORY.md`。

---

### 5. `.agent/`

放本地 skill / agent 工作流真源。

当前这里主要负责：

- 本仓开发循环
- 调试方法
- 实验/开发时的固定执行顺序

---

### 6. `docs/`

只放文档真源。

子层：

- `docs/architecture/`：正式结构、模块职责、开发流程
- `docs/experiments/`：已做实验的规格与证据边界

规则：

- 架构真源进 `architecture`
- 实验真源进 `experiments`
- 不把结构说明散落到 `note.md`

---

### 7. `scripts/`

统一脚本入口。

固定三层：

- `scripts/dev/`
- `scripts/verify/`
- `scripts/release/`

规则：

- 模块不在根目录乱放脚本
- 构建、安装、验证、发布都从这里进

---

### 8. `services/`

放桌面侧 / Mac 侧服务。

当前主模块：

- `services/mac-daemon/`

它只放：

- 本地服务端
- run artifact 落盘
- 命令分发
- 回放/比对

---

### 9. `explore/`

放探索期原型，不等于正式产品。

当前主模块：

- `explore/android-daemon-lab/`

规则：

- 允许薄实验
- 允许探针代码
- 不把正式产品职责长期堆在这里

---

### 10. `apps/`

放正式产品容器。

当前主模块：

- `apps/android-agent/`

规则：

- 与 `explore/` 分离
- 正式 Android 产品能力从这里长

---

### 11. `packages/`

放跨平台共享真源。

当前建议固定子目录：

```text
packages/
├─ foundation/
├─ blocks/
├─ flows/
├─ protocol/
├─ page-model/
├─ config/
└─ regression-fixtures/
```

职责：

- `foundation/`：纯函数、共用工具、无流程状态
- `blocks/`：最小能力块
- `flows/`：任务流程编排
- `protocol/`：双端通信协议
- `page-model/`：PageState / filter / anchor / workflow schema
- `config/`：server profile、版本等共享配置
- `regression-fixtures/`：共享回归样本

---

### 12. `artifacts/`

放真实运行证据：

- screenshot
- accessibility dump
- response
- logs
- manifest

规则：

- 只放运行产物
- 不放真源代码

---

## 三、模块落位规则

### Rule 1：跨平台 schema 一律进 `packages/`

例如：

- operation schema
- observer/filter schema
- anchor schema
- workflow schema
- protocol message schema

---

### Rule 2：平台实现放各自平台目录

- Mac 实现：`services/mac-daemon/`
- Android 探索实现：`explore/android-daemon-lab/`
- Android 正式实现：`apps/android-agent/`

不要把 Android 运行时代码放进 `packages/`。

---

### Rule 3：新能力先判断属于 foundation / blocks / flows 哪层

例如：

- 坐标计算、bounds helper -> `foundation`
- tap 执行块、page observe 块 -> `blocks`
- open-search、dismiss-blocker、enter-and-submit -> `flows`

---

### Rule 4：测试代码优先跟着模块走，样本统一进 `regression-fixtures`

- 单元测试：跟模块走
- 集成测试：跟实现模块走
- 共享样本：进 `packages/regression-fixtures/`

---

### Rule 5：脚本只能放 `scripts/`

禁止：

- 各模块根目录散落 `run.sh`
- 顶层到处堆 `test_*.py`
- 构建命令写在 note 里充当流程真源

---

## 四、正式模块模板

新增一个正式模块时，至少具备：

```text
<module>/
├─ src/
├─ tests/
├─ README.md
└─ module.md            # 可选；复杂模块才需要
```

如果模块比较复杂，再在 `src/` 下拆：

```text
src/
├─ foundation/
├─ blocks/
└─ flows/
```

---

## 五、当前接受的 workspace 结论

1. 结构真源以 `docs/architecture/` 为准，不再继续口头漂移。
2. `services/`、`explore/`、`apps/`、`packages/` 是四大代码区。
3. `packages/` 是共享真源区，不放平台运行时代码。
4. `scripts/` 是唯一脚本入口区。
5. `MEMORY.md + CACHE.md + note.md + AGENTS.md` 分别承担：
   - 长期经验
   - 短期现场
   - 探索过程
   - 稳定事实
