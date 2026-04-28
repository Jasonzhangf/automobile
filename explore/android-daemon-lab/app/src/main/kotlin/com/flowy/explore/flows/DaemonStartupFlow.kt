package com.flowy.explore.flows

import android.content.Context
import com.flowy.explore.blocks.AppendLogBlock
import com.flowy.explore.blocks.BackBlock
import com.flowy.explore.blocks.CaptureScreenshotBlock
import com.flowy.explore.blocks.ConnectWsBlock
import com.flowy.explore.blocks.SelfExemptionBlock
import com.flowy.explore.blocks.WsWatchdogBlock
import com.flowy.explore.blocks.DumpUiTreeRootBlock
import com.flowy.explore.blocks.DumpAccessibilityTreeBlock
import com.flowy.explore.blocks.EmitEventBlock
import com.flowy.explore.blocks.ExecuteOperationBlock
import com.flowy.explore.blocks.HandleFetchLogsBlock
import com.flowy.explore.blocks.HandlePingBlock
import com.flowy.explore.blocks.InputTextBlock
import com.flowy.explore.blocks.SetClipboardBlock
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
  private val selfExemptionBlock = SelfExemptionBlock()
  private val setClipboardBlock = SetClipboardBlock(context)
  private lateinit var wsClientAdapter: WsClientAdapter
  private lateinit var wsWatchdogBlock: WsWatchdogBlock
  private val scheduler = java.util.concurrent.Executors.newSingleThreadScheduledExecutor()

  fun start(onStatus: (String) -> Unit, onHeartbeat: (String) -> Unit) {
    // exempt self from battery optimization using root
    try {
      val exemption = selfExemptionBlock.run("com.flowy.explore")
      appendLogBlock.info("self_exemption", "result=${exemption.success} detail=${exemption.detail}")
    } catch (t: Throwable) {
      appendLogBlock.error("self_exemption_failed", t.message ?: "unknown")
    }

    lateinit var pingFlow: PingResponseFlow
    lateinit var fetchLogsFlow: FetchLogsFlow
    lateinit var screenshotCaptureFlow: ScreenshotCaptureFlow
    lateinit var accessibilityDumpFlow: AccessibilityDumpFlow
    lateinit var rootScreenshotCaptureFlow: RootScreenshotCaptureFlow
    lateinit var rootWindowStateFlow: RootWindowStateFlow
    lateinit var operationRunFlow: OperationRunFlow
    lateinit var workflowStepFlow: WorkflowStepFlow
    lateinit var dumpUiTreeRootFlow: DumpUiTreeRootFlow
    wsClientAdapter = WsClientAdapter(
      onOpen = {
        reconnectFlow.reset()
        if (::wsWatchdogBlock.isInitialized) wsWatchdogBlock.markAlive()
        onStatus("connected")
        onHeartbeat(TimeHelper.now())
        appendLogBlock.info("ws_connect_succeeded", "connected to mac daemon")
        sendHello()
      },
      onMessage = { message ->
        if (::wsWatchdogBlock.isInitialized) wsWatchdogBlock.markAlive()
        // Ignore keepalive frames from server — they have no requestId/command
        val isKeepalive = try {
          org.json.JSONObject(message).optString("type") == "keepalive"
        } catch (_: Throwable) { false }
        if (!isKeepalive) {
          try {
            wsSessionFlow(
              pingFlow,
              fetchLogsFlow,
              screenshotCaptureFlow,
              accessibilityDumpFlow,
              rootScreenshotCaptureFlow,
              rootWindowStateFlow,
              dumpUiTreeRootFlow,
              operationRunFlow,
              workflowStepFlow,
            ).onMessage(message)
          } catch (t: Throwable) {
            appendLogBlock.error("ws_message_error", "failed to handle: ${t.message}")
          }
        }
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
      dumpUiTreeRootBlock = DumpUiTreeRootBlock(),
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
    dumpUiTreeRootFlow = DumpUiTreeRootFlow(
      appendLogBlock = appendLogBlock,
      versionReader = versionReader,
      displayInfoReader = DisplayInfoReader(context),
      dumpUiTreeRootBlock = DumpUiTreeRootBlock(),
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
        inputTextBlock = InputTextBlock(setClipboard = setClipboardBlock::run),
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
        inputTextBlock = InputTextBlock(setClipboard = setClipboardBlock::run),
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

    // start WS watchdog after connection attempt
    wsWatchdogBlock = WsWatchdogBlock(scheduler, wsClientAdapter)
    wsWatchdogBlock.markAlive()
    wsWatchdogBlock.start(
      checkIntervalSec = 20,
      staleThresholdSec = 45,
      onReconnect = {
        appendLogBlock.info("ws_watchdog_reconnect", "triggering reconnect from watchdog")
        onStatus("reconnecting")
        connect(onStatus)
      },
    )
  }

  fun close() {
    FlowyAccessibilityService.setAvailabilityListener(null)
    scheduler.shutdownNow()
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
    dumpUiTreeRootFlow: DumpUiTreeRootFlow,
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
      dumpUiTreeRootFlow,
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
        put("dump-ui-tree-root")
        put("run-root-command")
        put("run-workflow-step")
        // operations always available (root backend)
        put("tap")
        put("scroll")
        put("input-text")
        put("back")
        put("press-key")
        if (accessibilityStatusReader.isEnabled()) {
          put("dump-accessibility-tree")
          put("open-deep-link")
        }
      })
      put("sentAt", TimeHelper.now())
    }
    wsClientAdapter.send(hello.toString())
  }
}
