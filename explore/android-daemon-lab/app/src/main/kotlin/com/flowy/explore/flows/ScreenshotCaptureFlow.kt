package com.flowy.explore.flows

import android.os.Build
import com.flowy.explore.blocks.AppendLogBlock
import com.flowy.explore.blocks.CaptureScreenshotBlock
import com.flowy.explore.blocks.UploadArtifactBlock
import com.flowy.explore.foundation.PageContextBuilder
import com.flowy.explore.foundation.TimeHelper
import com.flowy.explore.foundation.VersionReader
import com.flowy.explore.foundation.WsClientAdapter
import com.flowy.explore.runtime.AccessibilitySnapshotStore
import org.json.JSONArray
import org.json.JSONObject

class ScreenshotCaptureFlow(
  private val appendLogBlock: AppendLogBlock,
  private val versionReader: VersionReader,
  private val captureScreenshotBlock: CaptureScreenshotBlock,
  private val uploadArtifactBlock: UploadArtifactBlock,
  private val wsClientAdapter: WsClientAdapter,
) {
  fun run(requestId: String, runId: String, command: String) {
    val startedAt = TimeHelper.now()
    appendLogBlock.info("screenshot_capture_started", "capturing screenshot", requestId, runId, command)
    try {
      val capture = captureScreenshotBlock.run()
      val snapshot = AccessibilitySnapshotStore.current()
      val screenshotMeta = JSONObject().apply {
        put("requestId", requestId)
        put("runId", runId)
        put("fileName", "screenshot.png")
        put("format", "png")
        put("widthPx", capture.displayInfo.widthPx)
        put("heightPx", capture.displayInfo.heightPx)
        put("captureMethod", "media-projection")
        put("displayId", capture.displayInfo.displayId)
      }
      val pageContext = PageContextBuilder.build(
        requestId = requestId,
        runId = runId,
        command = command,
        capturedAt = startedAt,
        displayInfo = capture.displayInfo,
        projectionReady = true,
        accessibilitySnapshot = snapshot,
      )
      val artifacts = JSONArray().apply {
        put(upload(requestId, runId, command, "screenshot", "screenshot.png", "image/png", capture.pngBytes))
        put(upload(requestId, runId, command, "screenshot-meta", "screenshot-meta.json", "application/json", screenshotMeta.toString(2).toByteArray()))
        put(upload(requestId, runId, command, "page-context", "page-context.json", "application/json", pageContext.toString(2).toByteArray()))
      }
      appendLogBlock.info("screenshot_capture_finished", "captured screenshot", requestId, runId, command)
      wsClientAdapter.send(successResponse(requestId, runId, command, startedAt, artifacts).toString())
      appendLogBlock.info("command_finished", "finished capture-screenshot", requestId, runId, command)
    } catch (throwable: Throwable) {
      appendLogBlock.error("screenshot_capture_failed", throwable.message ?: "capture failed", requestId, runId, command)
      wsClientAdapter.send(errorResponse(requestId, runId, command, startedAt, throwable).toString())
      appendLogBlock.error("command_failed", "capture-screenshot failed", requestId, runId, command)
    }
  }

  private fun upload(
    requestId: String,
    runId: String,
    command: String,
    kind: String,
    fileName: String,
    contentType: String,
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
        put("contentType", contentType)
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
      put("message", "screenshot")
    }
  }

  private fun errorResponse(requestId: String, runId: String, command: String, startedAt: String, throwable: Throwable): JSONObject {
    val finishedAt = TimeHelper.now()
    return baseResponse(requestId, runId, command, startedAt, finishedAt).apply {
      put("status", "error")
      put("artifacts", JSONArray())
      put("error", JSONObject().apply {
        put("code", throwable.message ?: "SCREENSHOT_CAPTURE_FAILED")
        put("message", throwable.message ?: "capture failed")
      })
      put("message", "capture-error")
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
