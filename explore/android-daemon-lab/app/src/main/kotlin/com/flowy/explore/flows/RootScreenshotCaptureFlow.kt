package com.flowy.explore.flows

import android.os.Build
import com.flowy.explore.blocks.AppendLogBlock
import com.flowy.explore.blocks.RootScreenshotBlock
import com.flowy.explore.blocks.RootWindowStateBlock
import com.flowy.explore.blocks.UploadArtifactBlock
import com.flowy.explore.foundation.DisplayInfoReader
import com.flowy.explore.foundation.TimeHelper
import com.flowy.explore.foundation.VersionReader
import com.flowy.explore.foundation.WsClientAdapter
import org.json.JSONArray
import org.json.JSONObject

class RootScreenshotCaptureFlow(
  private val appendLogBlock: AppendLogBlock,
  private val versionReader: VersionReader,
  private val displayInfoReader: DisplayInfoReader,
  private val rootScreenshotBlock: RootScreenshotBlock,
  private val rootWindowStateBlock: RootWindowStateBlock,
  private val uploadArtifactBlock: UploadArtifactBlock,
  private val wsClientAdapter: WsClientAdapter,
) {
  fun run(requestId: String, runId: String, command: String) {
    val startedAt = TimeHelper.now()
    appendLogBlock.info("root_screenshot_started", "capturing root screenshot", requestId, runId, command)
    try {
      val pngBytes = rootScreenshotBlock.run()
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
      val screenshotMeta = JSONObject().apply {
        put("requestId", requestId)
        put("runId", runId)
        put("fileName", "screenshot.png")
        put("format", "png")
        put("widthPx", displayInfo.widthPx)
        put("heightPx", displayInfo.heightPx)
        put("captureMethod", "root-screencap")
        put("displayId", displayInfo.displayId)
      }
      val artifacts = JSONArray().apply {
        put(upload(requestId, runId, command, "screenshot", "screenshot.png", "image/png", pngBytes))
        put(upload(requestId, runId, command, "screenshot-meta", "screenshot-meta.json", "application/json", screenshotMeta.toString(2).toByteArray()))
        put(upload(requestId, runId, command, "page-context", "page-context.json", "application/json", pageContext.toString(2).toByteArray()))
      }
      appendLogBlock.info("root_screenshot_finished", "captured root screenshot", requestId, runId, command)
      wsClientAdapter.send(baseResponse(requestId, runId, command, startedAt).apply {
        put("status", "ok")
        put("artifacts", artifacts)
        put("error", JSONObject.NULL)
        put("message", "root-screenshot")
      }.toString())
      appendLogBlock.info("command_finished", "finished $command", requestId, runId, command)
    } catch (throwable: Throwable) {
      appendLogBlock.error("root_screenshot_failed", throwable.message ?: "root screenshot failed", requestId, runId, command)
      wsClientAdapter.send(baseResponse(requestId, runId, command, startedAt).apply {
        put("status", "error")
        put("artifacts", JSONArray())
        put("error", JSONObject().apply {
          put("code", throwable.message ?: "ROOT_SCREENSHOT_FAILED")
          put("message", throwable.message ?: "root screenshot failed")
        })
        put("message", "root-screenshot-error")
      }.toString())
      appendLogBlock.error("command_failed", "$command failed", requestId, runId, command)
    }
  }

  private fun upload(requestId: String, runId: String, command: String, kind: String, fileName: String, contentType: String, content: ByteArray): JSONObject {
    appendLogBlock.info("artifact_upload_started", "uploading $fileName", requestId, runId, command)
    val stored = uploadArtifactBlock.run(JSONObject().apply {
      put("protocolVersion", "exp01")
      put("requestId", requestId)
      put("runId", runId)
      put("deviceId", Build.DEVICE)
      put("command", command)
      put("kind", kind)
      put("fileName", fileName)
      put("contentType", contentType)
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
