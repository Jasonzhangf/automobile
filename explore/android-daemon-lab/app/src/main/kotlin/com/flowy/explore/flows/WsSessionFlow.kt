package com.flowy.explore.flows

import com.flowy.explore.blocks.AppendLogBlock
import org.json.JSONObject

class WsSessionFlow(
  private val appendLogBlock: AppendLogBlock,
  private val pingResponseFlow: PingResponseFlow,
  private val fetchLogsFlow: FetchLogsFlow,
  private val screenshotCaptureFlow: ScreenshotCaptureFlow,
  private val accessibilityDumpFlow: AccessibilityDumpFlow,
  private val rootScreenshotCaptureFlow: RootScreenshotCaptureFlow,
  private val rootWindowStateFlow: RootWindowStateFlow,
  private val dumpUiTreeRootFlow: DumpUiTreeRootFlow,
  private val operationRunFlow: OperationRunFlow,
  private val workflowStepFlow: WorkflowStepFlow,
) {
  fun onMessage(text: String) {
    val json = JSONObject(text)
    val requestId = json.getString("requestId")
    val runId = json.getString("runId")
    val command = json.getString("command")
    val payload = json.optJSONObject("payload") ?: JSONObject()
    appendLogBlock.info("command_received", "received $command", requestId, runId, command)
    when (command) {
      "ping" -> pingResponseFlow.run(requestId, runId, command)
      "fetch-logs" -> fetchLogsFlow.run(requestId, runId, command, json.optJSONObject("payload")?.optInt("tail", 200) ?: 200)
      "capture-screenshot" -> screenshotCaptureFlow.run(requestId, runId, command)
      "dump-accessibility-tree" -> accessibilityDumpFlow.run(requestId, runId, command)
      "capture-screenshot-root" -> rootScreenshotCaptureFlow.run(requestId, runId, command)
      "dump-window-state-root" -> rootWindowStateFlow.run(requestId, runId, command)
      "dump-ui-tree-root" -> dumpUiTreeRootFlow.run(requestId, runId, command)
      "run-workflow-step" -> workflowStepFlow.run(requestId, runId, command, payload)
      "tap", "scroll", "input-text", "back", "press-key", "open-deep-link", "run-root-command" ->
        operationRunFlow.run(requestId, runId, command, payload)
      else -> appendLogBlock.error("command_failed", "unsupported command $command", requestId, runId, command)
    }
  }
}
