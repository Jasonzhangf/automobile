package com.flowy.explore.flows

import android.content.Context
import com.flowy.explore.blocks.AppendLogBlock
import com.flowy.explore.blocks.CaptureScreenshotBlock
import com.flowy.explore.blocks.ConnectWsBlock
import com.flowy.explore.blocks.DumpAccessibilityTreeBlock
import com.flowy.explore.blocks.HandleFetchLogsBlock
import com.flowy.explore.blocks.HandlePingBlock
import com.flowy.explore.blocks.ReadLogTailBlock
import com.flowy.explore.blocks.UploadArtifactBlock
import com.flowy.explore.foundation.AccessibilityStatusReader
import com.flowy.explore.foundation.DevServerReader
import com.flowy.explore.foundation.DisplayInfoReader
import com.flowy.explore.foundation.TimeHelper
import com.flowy.explore.foundation.VersionReader
import com.flowy.explore.foundation.WsClientAdapter
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
    wsClientAdapter = WsClientAdapter(
      onOpen = {
        reconnectFlow.reset()
        onStatus("connected")
        onHeartbeat(TimeHelper.now())
        appendLogBlock.info("ws_connect_succeeded", "connected to mac daemon")
        sendHello()
      },
      onMessage = { message -> wsSessionFlow(pingFlow, fetchLogsFlow, screenshotCaptureFlow, accessibilityDumpFlow).onMessage(message) },
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
    screenshotCaptureFlow = ScreenshotCaptureFlow(
      appendLogBlock = appendLogBlock,
      versionReader = versionReader,
      captureScreenshotBlock = CaptureScreenshotBlock(context),
      uploadArtifactBlock = UploadArtifactBlock(DevServerReader(context).read()),
      wsClientAdapter = wsClientAdapter,
    )
    accessibilityDumpFlow = AccessibilityDumpFlow(
      appendLogBlock = appendLogBlock,
      versionReader = versionReader,
      accessibilityStatusReader = accessibilityStatusReader,
      dumpAccessibilityTreeBlock = DumpAccessibilityTreeBlock(),
      displayInfoReader = DisplayInfoReader(context),
      uploadArtifactBlock = UploadArtifactBlock(DevServerReader(context).read()),
      wsClientAdapter = wsClientAdapter,
    )
    connect(onStatus)
  }

  fun close() {
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
  ): WsSessionFlow {
    return WsSessionFlow(appendLogBlock, pingFlow, fetchLogsFlow, screenshotCaptureFlow, accessibilityDumpFlow)
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
        if (accessibilityStatusReader.isEnabled()) {
          put("dump-accessibility-tree")
        }
      })
      put("sentAt", TimeHelper.now())
    }
    wsClientAdapter.send(hello.toString())
  }
}
