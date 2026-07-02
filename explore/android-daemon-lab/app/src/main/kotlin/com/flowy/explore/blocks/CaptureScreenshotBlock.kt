package com.flowy.explore.blocks

import com.flowy.explore.foundation.executor.ProjectionCapture
import com.flowy.explore.runtime.adapter.MediaProjectionCaptureAdapter
import org.json.JSONObject

class CaptureScreenshotBlock(
  private val projectionCapture: ProjectionCapture = MediaProjectionCaptureAdapter,
) {
  fun run(payload: JSONObject): String {
    val filePath = payload.optString("filePath", "")
    check(filePath.isNotBlank()) { "MISSING_FILE_PATH" }
    check(projectionCapture.captureToFile(filePath)) { "SCREENSHOT_FAILED" }
    return "screenshot:$filePath"
  }
}
