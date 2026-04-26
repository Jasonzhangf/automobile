package com.flowy.explore.flows

import android.os.Build
import com.flowy.explore.blocks.AppendLogBlock
import com.flowy.explore.blocks.ExecuteOperationBlock
import com.flowy.explore.foundation.TimeHelper
import com.flowy.explore.foundation.VersionReader
import com.flowy.explore.foundation.WsClientAdapter
import org.json.JSONArray
import org.json.JSONObject

class OperationRunFlow(
  private val logInfo: (String, String, String, String, String) -> Unit,
  private val logError: (String, String, String, String, String) -> Unit,
  private val versionName: () -> String,
  private val sendResponse: (String) -> Boolean,
  private val executeOperation: (String, JSONObject, String) -> JSONObject,
  private val deviceInfo: () -> Triple<String, String, String> = { Triple(Build.DEVICE, Build.MODEL, Build.VERSION.RELEASE) },
) {
  constructor(
    appendLogBlock: AppendLogBlock,
    versionReader: VersionReader,
    wsClientAdapter: WsClientAdapter,
    executeOperationBlock: ExecuteOperationBlock,
  ) : this(
    logInfo = { event, message, requestId, runId, command -> appendLogBlock.info(event, message, requestId, runId, command) },
    logError = { event, message, requestId, runId, command -> appendLogBlock.error(event, message, requestId, runId, command) },
    versionName = versionReader::versionName,
    sendResponse = wsClientAdapter::send,
    executeOperation = executeOperationBlock::run,
  )

  fun run(requestId: String, runId: String, command: String, payload: JSONObject) {
    val startedAt = TimeHelper.now()
    logInfo("operation_started", "starting $command", requestId, runId, command)
    try {
      val blockResult = executeOperation(command, payload, command)
      if (blockResult.getString("status") != "ok") {
        val error = blockResult.optJSONObject("error")
        val code = error?.optString("code").orEmpty().ifBlank { "OPERATION_FAILED" }
        error(code)
      }
      val output = blockResult.optJSONObject("output") ?: JSONObject()
      val message = output.optString("message", command)
      sendResponse(response(requestId, runId, command, startedAt, "ok", message).toString())
      logInfo("operation_finished", "finished $command", requestId, runId, command)
      logInfo("command_finished", "finished $command", requestId, runId, command)
    } catch (throwable: Throwable) {
      sendResponse(response(requestId, runId, command, startedAt, "error", throwable.message ?: "OPERATION_FAILED").apply {
        put("error", JSONObject().apply {
          put("code", throwable.message ?: "OPERATION_FAILED")
          put("message", throwable.message ?: "OPERATION_FAILED")
        })
      }.toString())
      logError("operation_failed", throwable.message ?: "operation failed", requestId, runId, command)
      logError("command_failed", "$command failed", requestId, runId, command)
    }
  }

  private fun response(
    requestId: String,
    runId: String,
    command: String,
    startedAt: String,
    status: String,
    message: String,
  ): JSONObject {
    val finishedAt = TimeHelper.now()
    val (deviceId, model, androidVersion) = deviceInfo()
    return JSONObject().apply {
      put("protocolVersion", "exp01")
      put("requestId", requestId)
      put("runId", runId)
      put("command", command)
      put("status", status)
      put("startedAt", startedAt)
      put("finishedAt", finishedAt)
      put("durationMs", 0)
      put("device", JSONObject().apply {
        put("deviceId", deviceId)
        put("model", model)
        put("androidVersion", androidVersion)
      })
      put("app", JSONObject().apply {
        put("packageName", "com.flowy.explore")
        put("runtimeVersion", versionName())
      })
      put("artifacts", JSONArray())
      put("error", JSONObject.NULL)
      put("message", message)
    }
  }
}
