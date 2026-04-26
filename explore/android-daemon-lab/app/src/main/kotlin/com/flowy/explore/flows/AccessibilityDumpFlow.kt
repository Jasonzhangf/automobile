package com.flowy.explore.flows

import android.os.Build
import com.flowy.explore.blocks.AppendLogBlock
import com.flowy.explore.blocks.ObservePageBlock
import com.flowy.explore.blocks.UploadArtifactBlock
import com.flowy.explore.foundation.AccessibilityStatusReader
import com.flowy.explore.foundation.TimeHelper
import com.flowy.explore.foundation.VersionReader
import com.flowy.explore.foundation.WsClientAdapter
import org.json.JSONArray
import org.json.JSONObject

class AccessibilityDumpFlow(
  private val appendLogBlock: AppendLogBlock,
  private val versionReader: VersionReader,
  private val accessibilityStatusReader: AccessibilityStatusReader,
  private val observePageBlock: ObservePageBlock,
  private val uploadArtifactBlock: UploadArtifactBlock,
  private val wsClientAdapter: WsClientAdapter,
) {
  fun run(requestId: String, runId: String, command: String) {
    val startedAt = TimeHelper.now()
    appendLogBlock.info("accessibility_dump_started", "dumping accessibility tree", requestId, runId, command)
    try {
      if (!accessibilityStatusReader.isEnabled()) error("ACCESSIBILITY_SERVICE_DISABLED")
      val observedPage = observePageBlock.observe(
        requestId = requestId,
        runId = runId,
        command = command,
        observerSpec = JSONObject().put("requireAccessibility", true),
      )
      val snapshot = observedPage.accessibilitySnapshot ?: error("ACCESSIBILITY_ROOT_UNAVAILABLE")
      val artifacts = JSONArray().apply {
        put(upload(requestId, runId, command, "accessibility-raw", "accessibility-raw.json", snapshot.rawJson.toByteArray()))
        put(upload(requestId, runId, command, "page-context", "page-context.json", observedPage.pageContext.toString(2).toByteArray()))
      }
      appendLogBlock.info("accessibility_dump_finished", "dumped accessibility tree", requestId, runId, command)
      wsClientAdapter.send(successResponse(requestId, runId, command, startedAt, artifacts).toString())
      appendLogBlock.info("command_finished", "finished dump-accessibility-tree", requestId, runId, command)
    } catch (throwable: Throwable) {
      appendLogBlock.error("accessibility_dump_failed", throwable.message ?: "dump failed", requestId, runId, command)
      wsClientAdapter.send(errorResponse(requestId, runId, command, startedAt, throwable).toString())
      appendLogBlock.error("command_failed", "dump-accessibility-tree failed", requestId, runId, command)
    }
  }

  private fun upload(
    requestId: String,
    runId: String,
    command: String,
    kind: String,
    fileName: String,
    content: ByteArray,
  ): JSONObject {
    appendLogBlock.info("artifact_upload_started", "uploading $fileName", requestId, runId, command)
    val stored = uploadArtifactBlock.run(
      JSONObject().apply {
        put("protocolVersion", "exp01")
        put("requestId", requestId)
        put("runId", runId)
        put("deviceId", Build.DEVICE)
        put("command", command)
        put("kind", kind)
        put("fileName", fileName)
        put("contentType", "application/json")
      },
      content,
    )
    appendLogBlock.info("artifact_upload_finished", "uploaded $fileName", requestId, runId, command)
    return stored
  }

  private fun successResponse(requestId: String, runId: String, command: String, startedAt: String, artifacts: JSONArray): JSONObject {
    val finishedAt = TimeHelper.now()
    return baseResponse(requestId, runId, command, startedAt, finishedAt).apply {
      put("status", "ok")
      put("artifacts", artifacts)
      put("error", JSONObject.NULL)
      put("message", "accessibility-tree")
    }
  }

  private fun errorResponse(requestId: String, runId: String, command: String, startedAt: String, throwable: Throwable): JSONObject {
    val finishedAt = TimeHelper.now()
    return baseResponse(requestId, runId, command, startedAt, finishedAt).apply {
      put("status", "error")
      put("artifacts", JSONArray())
      put("error", JSONObject().apply {
        put("code", throwable.message ?: "ACCESSIBILITY_ROOT_UNAVAILABLE")
        put("message", throwable.message ?: "dump failed")
      })
      put("message", "accessibility-error")
    }
  }

  private fun baseResponse(requestId: String, runId: String, command: String, startedAt: String, finishedAt: String): JSONObject {
    return JSONObject().apply {
      put("protocolVersion", "exp01")
      put("requestId", requestId)
      put("runId", runId)
      put("command", command)
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
    }
  }
}
