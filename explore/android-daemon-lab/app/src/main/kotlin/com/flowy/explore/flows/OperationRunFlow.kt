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
  private val appendLogBlock: AppendLogBlock,
  private val versionReader: VersionReader,
  private val wsClientAdapter: WsClientAdapter,
  private val executeOperationBlock: ExecuteOperationBlock,
) {
  fun run(requestId: String, runId: String, command: String, payload: JSONObject) {
    val startedAt = TimeHelper.now()
    appendLogBlock.info("operation_started", "starting $command", requestId, runId, command)
    try {
      val blockResult = executeOperationBlock.run(command, payload, command)
      if (blockResult.getString("status") != "ok") {
        val error = blockResult.optJSONObject("error")
        val code = error?.optString("code").orEmpty().ifBlank { "OPERATION_FAILED" }
        error(code)
      }
      val output = blockResult.optJSONObject("output") ?: JSONObject()
      val message = output.optString("message", command)
      wsClientAdapter.send(response(requestId, runId, command, startedAt, "ok", message).toString())
      appendLogBlock.info("operation_finished", "finished $command", requestId, runId, command)
      appendLogBlock.info("command_finished", "finished $command", requestId, runId, command)
    } catch (throwable: Throwable) {
      wsClientAdapter.send(response(requestId, runId, command, startedAt, "error", throwable.message ?: "OPERATION_FAILED").apply {
        put("error", JSONObject().apply {
          put("code", throwable.message ?: "OPERATION_FAILED")
          put("message", throwable.message ?: "OPERATION_FAILED")
        })
      }.toString())
      appendLogBlock.error("operation_failed", throwable.message ?: "operation failed", requestId, runId, command)
      appendLogBlock.error("command_failed", "$command failed", requestId, runId, command)
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
        put("deviceId", Build.DEVICE)
        put("model", Build.MODEL)
        put("androidVersion", Build.VERSION.RELEASE)
      })
      put("app", JSONObject().apply {
        put("packageName", "com.flowy.explore")
        put("runtimeVersion", versionReader.versionName())
      })
      put("artifacts", JSONArray())
      put("error", JSONObject.NULL)
      put("message", message)
    }
  }
}
