package com.flowy.explore.flows

import android.os.Build
import com.flowy.explore.blocks.AppendLogBlock
import com.flowy.explore.blocks.RootWindowStateBlock
import com.flowy.explore.blocks.UploadArtifactBlock
import com.flowy.explore.foundation.DisplayInfoReader
import com.flowy.explore.foundation.TimeHelper
import com.flowy.explore.foundation.VersionReader
import com.flowy.explore.foundation.WsClientAdapter
import org.json.JSONArray
import org.json.JSONObject

class RootWindowStateFlow(
  private val appendLogBlock: AppendLogBlock,
  private val versionReader: VersionReader,
  private val displayInfoReader: DisplayInfoReader,
  private val rootWindowStateBlock: RootWindowStateBlock,
  private val uploadArtifactBlock: UploadArtifactBlock,
  private val wsClientAdapter: WsClientAdapter,
) {
  fun run(requestId: String, runId: String, command: String) {
    val startedAt = TimeHelper.now()
    appendLogBlock.info("root_window_state_started", "dumping root window state", requestId, runId, command)
    try {
      val windowState = rootWindowStateBlock.run()
      val displayInfo = displayInfoReader.read()
      val pageContext = JSONObject().apply {
        put("requestId", requestId)
        put("runId", runId)
        put("command", command)
        put("capturedAt", TimeHelper.now())
        put("app", JSONObject().apply {
          put("packageName", windowState.packageName ?: JSONObject.NULL)
          put("windowTitle", windowState.windowTitle ?: JSONObject.NULL)
        })
        put("screen", JSONObject().apply {
          put("widthPx", displayInfo.widthPx)
          put("heightPx", displayInfo.heightPx)
          put("rotation", displayInfo.rotation)
          put("densityDpi", displayInfo.densityDpi)
        })
        put("runtime", JSONObject().apply {
          put("projectionReady", false)
          put("accessibilityReady", false)
          put("rootReady", true)
        })
      }
      val stateJson = JSONObject().apply {
        put("packageName", windowState.packageName ?: JSONObject.NULL)
        put("windowTitle", windowState.windowTitle ?: JSONObject.NULL)
        put("rawText", windowState.rawText)
      }
      val artifacts = JSONArray().apply {
        put(upload(requestId, runId, command, "root-window-state", "root-window-state.json", stateJson.toString(2).toByteArray()))
        put(upload(requestId, runId, command, "page-context", "page-context.json", pageContext.toString(2).toByteArray()))
      }
      appendLogBlock.info("root_window_state_finished", "dumped root window state", requestId, runId, command)
      wsClientAdapter.send(baseResponse(requestId, runId, command, startedAt).apply {
        put("status", "ok")
        put("artifacts", artifacts)
        put("error", JSONObject.NULL)
        put("message", "root-window-state")
      }.toString())
      appendLogBlock.info("command_finished", "finished $command", requestId, runId, command)
    } catch (throwable: Throwable) {
      appendLogBlock.error("root_window_state_failed", throwable.message ?: "root window state failed", requestId, runId, command)
      wsClientAdapter.send(baseResponse(requestId, runId, command, startedAt).apply {
        put("status", "error")
        put("artifacts", JSONArray())
        put("error", JSONObject().apply {
          put("code", throwable.message ?: "ROOT_WINDOW_STATE_FAILED")
          put("message", throwable.message ?: "root window state failed")
        })
        put("message", "root-window-state-error")
      }.toString())
      appendLogBlock.error("command_failed", "$command failed", requestId, runId, command)
    }
  }

  private fun upload(requestId: String, runId: String, command: String, kind: String, fileName: String, content: ByteArray): JSONObject {
    appendLogBlock.info("artifact_upload_started", "uploading $fileName", requestId, runId, command)
    val stored = uploadArtifactBlock.run(JSONObject().apply {
      put("protocolVersion", "exp01")
      put("requestId", requestId)
      put("runId", runId)
      put("deviceId", Build.DEVICE)
      put("command", command)
      put("kind", kind)
      put("fileName", fileName)
      put("contentType", "application/json")
    }, content)
    appendLogBlock.info("artifact_upload_finished", "uploaded $fileName", requestId, runId, command)
    return stored
  }

  private fun baseResponse(requestId: String, runId: String, command: String, startedAt: String): JSONObject {
    return JSONObject().apply {
      put("protocolVersion", "exp01")
      put("requestId", requestId)
      put("runId", runId)
      put("command", command)
      put("startedAt", startedAt)
      put("finishedAt", TimeHelper.now())
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
