# Experiment 01 / Phase B — 文件树与模块落点

## 目标

把 Phase B 的新增文件树定清楚，避免：
- 把 screenshot / accessibility 混进 Phase A 的 transport 代码
- 把 sensor runtime 和 command runtime 混写
- 把 artifact upload 和 capture 逻辑揉成一个大文件

本文件只定义 **实验阶段** 的 Phase B 落点。

## 文档落点

```text
docs/experiments/
  experiment-01-phase-b-implementation-spec.md
  experiment-01-phase-b-file-layout.md
  exp01-json-truth-spec.md
```

## Android daemon-lab 新增目标文件树

```text
explore/
  android-daemon-lab/
    app/
      src/
        main/
          AndroidManifest.xml
          kotlin/com/flowy/explore/
            foundation/
              ProjectionStateStore
              BitmapEncoder
              DisplayInfoReader
              AccessibilityTreeSerializer
              AccessibilitySnapshotStore
            blocks/
              RequestProjectionPermissionBlock
              CaptureScreenshotBlock
              DumpAccessibilityTreeBlock
              UploadArtifactBlock
              ReadPageContextBlock
            flows/
              ScreenshotCaptureFlow
              AccessibilityDumpFlow
              ArtifactUploadFlow
            runtime/
              DaemonForegroundService
              FlowyAccessibilityService
              MediaProjectionSessionHolder
            ui/
              DevPanelActivity
```

## Android 各文件职责

### `foundation/ProjectionStateStore`
- 保存当前 projection 是否 ready
- 保存当前 session 基本元数据
- 不负责截图执行

### `foundation/BitmapEncoder`
- `Bitmap -> PNG bytes`
- 只做编码，不做上传

### `foundation/DisplayInfoReader`
- 读取屏幕宽高、density、rotation、displayId
- 为 `page-context.json` 和 `screenshot-meta.json` 提供数据

### `foundation/AccessibilityTreeSerializer`
- 将 `AccessibilityNodeInfo` 树序列化为 Phase B 原始 JSON
- 只做序列化，不做 service 生命周期控制

### `foundation/AccessibilitySnapshotStore`
- 持有最近一次可用 root snapshot / metadata
- 给 `DumpAccessibilityTreeBlock` 提供读取入口

### `blocks/RequestProjectionPermissionBlock`
- 仅用于从可见 UI 触发 projection 授权流程
- 不参与后台命令编排

### `blocks/CaptureScreenshotBlock`
- 执行一次截图
- 输出 `screenshot.png` 与 `screenshot-meta.json` 所需原始数据
- 不负责上传

### `blocks/DumpAccessibilityTreeBlock`
- 执行一次 raw tree 导出
- 输出 `accessibility-raw.json` 所需数据
- 不负责上传

### `blocks/UploadArtifactBlock`
- 上传单个 artifact 到 Mac daemon
- 失败必须返回结构化错误

### `blocks/ReadPageContextBlock`
- 组装 `page-context.json`
- 汇总 package / screen / runtime / source metadata

### `flows/ScreenshotCaptureFlow`
- command 校验
- 调用 screenshot block
- 组装 page-context
- 串联上传
- 生成 response

### `flows/AccessibilityDumpFlow`
- command 校验
- 调用 accessibility dump block
- 组装 page-context
- 串联上传
- 生成 response

### `flows/ArtifactUploadFlow`
- 统一 artifact 上传顺序与失败处理
- 供 screenshot / accessibility 两条流程复用

### `runtime/FlowyAccessibilityService`
- 监听系统 accessibility 能力
- 提供 active window root 或最近 snapshot
- 不负责 WebSocket transport

### `runtime/MediaProjectionSessionHolder`
- 保存当前运行期内可用的 projection session
- 统一 session 生命周期

### `ui/DevPanelActivity`
Phase B 最少新增这些可见入口：
- `Grant Screenshot Permission`
- `Open Accessibility Settings`
- 当前 projection 状态
- 当前 accessibility 状态

## Mac daemon 新增目标文件树

```text
services/
  mac-daemon/
    src/
      foundation/
        multipart_artifact
        sha256
      blocks/
        receive_artifact_upload
        persist_binary_artifact
        persist_json_artifact
        persist_page_context_artifact
      flows/
        artifact_upload_flow
        finalize_run_flow
```

## Mac 各文件职责

### `foundation/multipart_artifact`
- 解析 `multipart/form-data`
- 提取 `meta` 与 `file`

### `foundation/sha256`
- 计算落盘文件摘要
- 返回给 artifact descriptor / manifest

### `blocks/receive_artifact_upload`
- 接收 Phase B artifact 上传请求
- 校验字段完整性

### `blocks/persist_binary_artifact`
- 保存 `screenshot.png` 这类二进制文件

### `blocks/persist_json_artifact`
- 保存 `accessibility-raw.json`、`screenshot-meta.json`

### `blocks/persist_page_context_artifact`
- 专门保存 `page-context.json`
- 这样后面做 bundle 对齐时不会和普通 JSON artifact 混掉语义

### `flows/artifact_upload_flow`
- 管理上传校验、落盘、descriptor 返回

### `flows/finalize_run_flow`
- 继续作为 run 汇总出口
- 增补 screenshot / accessibility 相关文件到 `manifest.json`

## scripts 建议新增入口

```text
scripts/
  dev/
    phase-b-build-android-lab.sh
    phase-b-install-android-lab.sh
    phase-b-send-capture-screenshot.sh
    phase-b-send-dump-accessibility-tree.sh
  verify/
    phase-b-verify-android-lab.sh
    phase-b-verify-mac-artifacts.sh
```

## 文件落点原则

### 1. screenshot / accessibility 不能直塞进 `DaemonForegroundService`
前台服务只负责：
- lifecycle
- transport
- flow dispatch

真正的 capture / dump / upload 必须下沉到 block / flow。

### 2. `page-context.json` 必须作为独立 artifact
不能只把 page context 塞进 response envelope 里。

原因：
- 它需要被人工单独查看
- 它后面会变成 screenshot/tree/page-model 对齐锚点
- 它应该和大文件 artifact 一起进入 run bundle

### 3. sensor runtime 与 command runtime 必须分离
- `DaemonForegroundService`：命令通道与后台运行
- `FlowyAccessibilityService`：感知事件与 root snapshot
- `MediaProjectionSessionHolder`：截图授权 session

三者可以协作，但不能混成一个类。
