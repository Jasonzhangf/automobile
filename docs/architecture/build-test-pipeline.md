# Build Test Pipeline v1

## 目标

把 Flowy 的构建流程固定成一条正式流水线：

```text
bump version
-> file-line gate
-> tests
-> build artifact
-> post-build smoke
```

规则：

- **每次 build 自动 bump 版本**
- **每次 build 后必须自动跑测试**
- 没有测试通过，不算 build 成功

---

## 一、当前接受的正式规则

### 1. 版本规则

Android 侧版本从：

```text
0.1.0001
```

开始。

每次 build：

- 自动把 `buildNumber + 1`
- 自动更新 `versionName`

当前版本真源：

```text
explore/android-daemon-lab/config/runtime-version.json
```

后续正式产品阶段再提升到共享 `packages/config/version/`。

---

### 2. 测试规则

每次 build 必须自动执行：

1. 文件行数门禁
2. 单元测试
3. block 覆盖测试
4. 相关回归测试
5. 构建产物检查

---

### 3. 入口规则

构建与验证不能散在各处。

固定入口：

- `scripts/dev/`：构建与本地开发
- `scripts/verify/`：验证与回归

---

## 二、Android lab 当前流水线

当前探索 Android runtime 的正式流水线定义为：

```text
scripts/dev/build-android-lab.sh
-> Gradle preBuild
   -> bumpRuntimeVersion
   -> checkFileLines
-> testDebugUnitTest
-> blocks coverage checks
-> assembleDebug
-> apk exists smoke
```

说明：

- `bumpRuntimeVersion` 在 Gradle `preBuild` 阶段自动执行
- `assembleDebug` 依赖 `testDebugUnitTest`
- 构建脚本再追加最小产物 smoke

---

## 三、当前脚本职责

### 1. `scripts/dev/build-android-lab.sh`

职责：

- 启动 Android lab 的正式构建入口
- 读取 build 前版本
- 触发完整构建
- 输出 build 后版本
- 检查 APK 是否生成

这是后续默认使用的 Android lab build 入口。

---

### 2. `scripts/verify/android-lab-build-pipeline-smoke.sh`

职责：

- 验证构建流水线本身是通的
- 至少确认：
  - 版本有 bump
  - 测试未报错
  - APK 产物存在

这是“流水线验证”，不是业务功能验证。

---

### 3. `scripts/verify/check-file-lines.sh`

职责：

- 保证所有源码文件不超过 500 行

这是第一门禁。

---

## 四、构建成功判据

只有同时满足下面条件，build 才算成功：

1. `runtime-version.json` 已自动 bump
2. `check-file-lines` 通过
3. `testDebugUnitTest` 通过
4. block 覆盖验证通过
5. `app-debug.apk` 已生成

任意一项失败：

- build 失败
- 不允许报告“已完成构建”

---

## 五、后续正式产品阶段的推广

等 `apps/android-agent/` 开工后，沿用同一模式：

```text
version truth
-> line gate
-> unit tests
-> regression
-> build
-> smoke
```

不要再为正式产品重新发明另一套 build 规则。

---

## 六、当前接受的设计结论

1. 每次 build 自动 bump 版本。
2. 每次 build 自动带测试，不接受 compile-only。
3. block 必须先做单测 + 覆盖测试，再做 flow 编排测试。
4. 构建入口与验证入口必须走 `scripts/dev` 与 `scripts/verify`。
5. 先把 Android lab 的 build pipeline 固化，再推广到正式 Android agent。
