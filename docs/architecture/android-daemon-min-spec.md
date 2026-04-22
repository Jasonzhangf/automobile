# Android Daemon Minimum Spec v0.1

## 目标

当前探索阶段，Android 侧的**最小实验对象**不是正式 APP 全量功能，而是一个可验证的 **long-running daemon runtime**。

它必须先证明这 5 件事：

1. 能长时间后台运行
2. 能接收来自 Mac 端 Agent / daemon 的命令
3. 能执行最小动作或最小感知任务
4. 能把结果 / 状态 / 错误反馈回 Mac 端
5. 能本地记录日志，便于断线后排查

## 当前阶段定位

- 这是 **exploration runtime**，不是正式 APP 脚手架
- 只服务于实验闭环：`receive command -> execute -> report -> log`
- 只要最小链路未验证，不扩张到正式功能层
- 正式产品 UI 形态单独定义在 `docs/architecture/android-overlay-ui-spec.md`

## 最小能力范围

### 1. Runtime lifecycle
- 可启动
- 可进入后台
- 可长时保持运行
- 可显示当前状态（idle / busy / error / disconnected）
- 可被用户可见地停止或重启
- 正式产品中由 `Foreground Service` 承担常驻主干；悬浮球只承担 UI 入口

### 2. Command channel
- 接收 Mac 端命令
- 命令至少支持：
  - ping
  - capture-page
  - capture-screenshot
  - dump-accessibility-tree
  - fetch-logs
- 命令必须带 request id / timestamp
- 返回结果必须带 status / error / duration

### 3. Execution
- 先不追求复杂操作
- 当前最小执行以 **感知与反馈** 为主：
  - 页面信息采集
  - 截图
  - accessibility dump
  - 本地日志落盘

### 4. Feedback
- 结果必须能回传到 Mac daemon
- 失败必须带明确错误原因
- 反馈对象至少包含：
  - request id
  - device/app metadata
  - result status
  - artifact path or payload reference
  - error message if failed

### 5. Local logging
- 本地必须保留日志
- 日志至少包含：
  - lifecycle event
  - command receive/start/end
  - network error
  - capture error
  - upload result
- 日志必须支持被拉取或打包进 run bundle

## 目录边界（仅设计，不代表现在开工）

探索代码与正式 APP 必须分开：

```text
explore/
  android-daemon-lab/     # 当前实验对象
apps/
  android-agent/          # 后续正式 APP / 产品化容器
```

规则：
- `explore/android-daemon-lab/`：允许薄实验、最小验证、临时探针
- `apps/android-agent/`：只在实验闭环后再开始正式脚手架与基础功能

## 三层代码规则

所有实现都遵守三层拆分：

### Layer 1: foundation
公用函数 / 纯工具 / 无业务状态依赖

例子：
- json/file helpers
- timestamp/version helpers
- checksum / hash
- retry / timeout helpers
- logger primitives

### Layer 2: blocks
最小能力编排块；每个 block 只完成一个原子能力

例子：
- receive-command block
- capture-screenshot block
- dump-accessibility block
- upload-result block
- persist-log block

要求：
- block 不承载长流程决策
- block 只做一个能力闭环

### Layer 3: flows
任务流程编排；把多个 blocks 串成任务

例子：
- receive-command -> capture -> upload -> ack
- receive-command -> dump-tree -> persist -> upload
- periodic-health-report flow

要求：
- flow 只做流程与状态机
- flow 不复制 foundation / block 逻辑

## 版本规则

版本从：

```text
0.1.0001
```

开始。

规则：
- 前两段表示阶段版本：`0.1`
- 第四位块为 **4 位 build number**
- **每次编译自动 bump build number**
- 不允许手工忘记 bump

示例：

```text
0.1.0001
0.1.0002
0.1.0003
```

## 版本真源（设计）

后续正式脚手架阶段建议提供：

```text
packages/config/version/
  flowy-version.json
```

由 build 脚本在每次编译前自动更新。

当前探索阶段先把此规则记录为事实，不急着实现脚手架。

## 编译门禁

任何一次编译都必须自动执行：

1. 文件行数检查（<= 500）
2. 单元测试 / block 级验证
3. 回归测试
4. 编译产物生成
5. 最小 smoke verification

编译成功但未完成回归，视为失败。

## 当前实验退出条件

只有当下面最小闭环真实成立后，才进入正式脚手架阶段：

1. Android daemon 能后台稳定运行一段时间
2. Mac daemon 能下发至少一条真实命令
3. Android 端能执行至少一个真实 capture 任务
4. 结果能回传到 Mac 端并落盘
5. 本地日志可被取回并用于排错
