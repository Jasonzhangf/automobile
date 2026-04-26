package com.flowy.explore.flows

import android.content.Context
import com.flowy.explore.blocks.AppendLogBlock
import com.flowy.explore.blocks.BackBlock
import com.flowy.explore.blocks.CaptureScreenshotBlock
import com.flowy.explore.blocks.ConnectWsBlock
import com.flowy.explore.blocks.DumpAccessibilityTreeBlock
import com.flowy.explore.blocks.EmitEventBlock
import com.flowy.explore.blocks.ExecuteOperationBlock
import com.flowy.explore.blocks.HandleFetchLogsBlock
import com.flowy.explore.blocks.HandlePingBlock
import com.flowy.explore.blocks.InputTextBlock
import com.flowy.explore.blocks.ObservePageBlock
import com.flowy.explore.blocks.OpenDeepLinkBlock
import com.flowy.explore.blocks.PressKeyBlock
import com.flowy.explore.blocks.ReadLogTailBlock
import com.flowy.explore.blocks.RootCommandBlock
import com.flowy.explore.blocks.RootWindowStateBlock
import com.flowy.explore.blocks.RootScreenshotBlock
import com.flowy.explore.blocks.ScrollBlock
import com.flowy.explore.blocks.TapBlock
import com.flowy.explore.blocks.UploadArtifactBlock
import com.flowy.explore.foundation.AccessibilityStatusReader
import com.flowy.explore.foundation.DevServerReader
import com.flowy.explore.foundation.DisplayInfoReader
import com.flowy.explore.foundation.TimeHelper
import com.flowy.explore.foundation.VersionReader
import com.flowy.explore.foundation.WsClientAdapter
import com.flowy.explore.runtime.FlowyAccessibilityService
import com.flowy.explore.runtime.LocalLogStore
import org.json.JSONArray
import org.json.JSONObject

class DaemonStartupFlow(
  private val context: Context,
  private val logStore: LocalLogStore,
  private val reconnectFlow: ReconnectFlow,
) {
  private val appendLogBlock = AppendLogBlock(logStore)
  private val versionReader = VersionReader(context)
  private val accessibilityStatusReader = AccessibilityStatusReader(context)
  private lateinit var wsClientAdapter: WsClientAdapter

  fun start(onStatus: (String) -> Unit, onHeartbeat: (String) -> Unit) {
    lateinit var pingFlow: PingResponseFlow
    lateinit var fetchLogsFlow: FetchLogsFlow
    lateinit var screenshotCaptureFlow: ScreenshotCaptureFlow
    lateinit var accessibilityDumpFlow: AccessibilityDumpFlow
    lateinit var rootScreenshotCaptureFlow: RootScreenshotCaptureFlow
    lateinit var rootWindowStateFlow: RootWindowStateFlow
    lateinit var operationRunFlow: OperationRunFlow
    lateinit var workflowStepFlow: WorkflowStepFlow
    wsClientAdapter = WsClientAdapter(
      onOpen = {
        reconnectFlow.reset()
        onStatus("connected")
        onHeartbeat(TimeHelper.now())
        appendLogBlock.info("ws_connect_succeeded", "connected to mac daemon")
        sendHello()
      },
      onMessage = { message ->
        wsSessionFlow(
          pingFlow,
          fetchLogsFlow,
          screenshotCaptureFlow,
          accessibilityDumpFlow,
          rootScreenshotCaptureFlow,
          rootWindowStateFlow,
          operationRunFlow,
          workflowStepFlow,
        ).onMessage(message)
      },
      onClosing = {
        onStatus("closing")
        appendLogBlock.info("ws_reconnect_scheduled", "websocket closing; scheduling reconnect")
        reconnectFlow.schedule { connect(onStatus) }
      },
      onFailure = { throwable ->
        onStatus("error")
        appendLogBlock.error("ws_connect_failed", throwable.message ?: "ws failure")
        reconnectFlow.schedule { connect(onStatus) }
      },
    )
    pingFlow = PingResponseFlow(appendLogBlock, HandlePingBlock(versionReader), wsClientAdapter)
    fetchLogsFlow = FetchLogsFlow(appendLogBlock, HandleFetchLogsBlock(versionReader, ReadLogTailBlock(logStore)), wsClientAdapter)
    val observePageBlock = ObservePageBlock(
      displayInfoReader = DisplayInfoReader(context),
      dumpAccessibilityTreeBlock = DumpAccessibilityTreeBlock(),
      captureScreenshotBlock = CaptureScreenshotBlock(),
    )
    screenshotCaptureFlow = ScreenshotCaptureFlow(
      appendLogBlock = appendLogBlock,
      versionReader = versionReader,
      observePageBlock = observePageBlock,
      uploadArtifactBlock = UploadArtifactBlock(DevServerReader(context).read()),
      wsClientAdapter = wsClientAdapter,
    )
    accessibilityDumpFlow = AccessibilityDumpFlow(
      appendLogBlock = appendLogBlock,
      versionReader = versionReader,
      accessibilityStatusReader = accessibilityStatusReader,
      observePageBlock = observePageBlock,
      uploadArtifactBlock = UploadArtifactBlock(DevServerReader(context).read()),
      wsClientAdapter = wsClientAdapter,
    )
    rootScreenshotCaptureFlow = RootScreenshotCaptureFlow(
      appendLogBlock = appendLogBlock,
      versionReader = versionReader,
      displayInfoReader = DisplayInfoReader(context),
      rootScreenshotBlock = RootScreenshotBlock(),
      rootWindowStateBlock = RootWindowStateBlock(),
      uploadArtifactBlock = UploadArtifactBlock(DevServerReader(context).read()),
      wsClientAdapter = wsClientAdapter,
    )
    rootWindowStateFlow = RootWindowStateFlow(
      appendLogBlock = appendLogBlock,
      versionReader = versionReader,
      displayInfoReader = DisplayInfoReader(context),
      rootWindowStateBlock = RootWindowStateBlock(),
      uploadArtifactBlock = UploadArtifactBlock(DevServerReader(context).read()),
      wsClientAdapter = wsClientAdapter,
    )
    operationRunFlow = OperationRunFlow(
      appendLogBlock = appendLogBlock,
      versionReader = versionReader,
      wsClientAdapter = wsClientAdapter,
      executeOperationBlock = ExecuteOperationBlock(
        tapBlock = TapBlock(),
        scrollBlock = ScrollBlock(),
        inputTextBlock = InputTextBlock(),
        backBlock = BackBlock(),
        pressKeyBlock = PressKeyBlock(),
        openDeepLinkBlock = OpenDeepLinkBlock(context),
        rootCommandBlock = RootCommandBlock(),
      ),
    )
    workflowStepFlow = WorkflowStepFlow(
      logInfo = { event, message, requestId, runId, command -> appendLogBlock.info(event, message, requestId, runId, command) },
      logError = { event, message, requestId, runId, command -> appendLogBlock.error(event, message, requestId, runId, command) },
      versionName = versionReader::versionName,
      sendResponse = wsClientAdapter::send,
      observePage = observePageBlock::observe,
      filterTargets = com.flowy.explore.blocks.FilterTargetsBlock()::run,
      evaluateAnchor = com.flowy.explore.blocks.EvaluateAnchorBlock()::run,
      executeOperation = ExecuteOperationBlock(
        tapBlock = TapBlock(),
        scrollBlock = ScrollBlock(),
        inputTextBlock = InputTextBlock(),
        backBlock = BackBlock(),
        pressKeyBlock = PressKeyBlock(),
        openDeepLinkBlock = OpenDeepLinkBlock(context),
        rootCommandBlock = RootCommandBlock(),
      )::run,
      emitEvent = EmitEventBlock(
        sender = { event ->
          appendLogBlock.info("workflow_event", event)
          true
        },
      )::run,
    )
    FlowyAccessibilityService.setAvailabilityListener { sendHello() }
    connect(onStatus)
  }

  fun close() {
    FlowyAccessibilityService.setAvailabilityListener(null)
    if (::wsClientAdapter.isInitialized) wsClientAdapter.close()
  }

  private fun connect(onStatus: (String) -> Unit) {
    val config = DevServerReader(context).read()
    onStatus("connecting")
    appendLogBlock.info("ws_connect_started", "connecting to ${config.wsUrl()}")
    ConnectWsBlock(wsClientAdapter).run(config.wsUrl())
  }

  private fun wsSessionFlow(
    pingFlow: PingResponseFlow,
    fetchLogsFlow: FetchLogsFlow,
    screenshotCaptureFlow: ScreenshotCaptureFlow,
    accessibilityDumpFlow: AccessibilityDumpFlow,
    rootScreenshotCaptureFlow: RootScreenshotCaptureFlow,
    rootWindowStateFlow: RootWindowStateFlow,
    operationRunFlow: OperationRunFlow,
    workflowStepFlow: WorkflowStepFlow,
  ): WsSessionFlow {
    return WsSessionFlow(
      appendLogBlock,
      pingFlow,
      fetchLogsFlow,
      screenshotCaptureFlow,
      accessibilityDumpFlow,
      rootScreenshotCaptureFlow,
      rootWindowStateFlow,
      operationRunFlow,
      workflowStepFlow,
    )
  }

  private fun sendHello() {
    val hello = JSONObject().apply {
      put("type", "client_hello")
      put("protocolVersion", "exp01")
      put("deviceId", android.os.Build.DEVICE)
      put("runtimeVersion", versionReader.versionName())
      put("appId", "com.flowy.explore")
      put("capabilities", JSONArray().apply {
        put("ping")
        put("fetch-logs")
        put("capture-screenshot")
        put("capture-screenshot-root")
        put("dump-window-state-root")
        put("run-root-command")
        if (accessibilityStatusReader.isEnabled()) {
          put("dump-accessibility-tree")
          put("tap")
          put("scroll")
          put("input-text")
          put("back")
          put("press-key")
          put("open-deep-link")
          put("run-workflow-step")
        }
      })
      put("sentAt", TimeHelper.now())
    }
    wsClientAdapter.send(hello.toString())
  }
}
