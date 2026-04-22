package com.flowy.explore.foundation

import com.flowy.explore.runtime.AccessibilitySnapshot
import org.json.JSONObject

object PageContextBuilder {
  fun build(
    requestId: String,
    runId: String,
    command: String,
    capturedAt: String,
    displayInfo: DisplayInfo,
    projectionReady: Boolean,
    accessibilitySnapshot: AccessibilitySnapshot?,
  ): JSONObject {
    return JSONObject().apply {
      put("requestId", requestId)
      put("runId", runId)
      put("command", command)
      put("capturedAt", capturedAt)
      put("app", JSONObject().apply {
        put("packageName", accessibilitySnapshot?.packageName ?: JSONObject.NULL)
        put("windowTitle", accessibilitySnapshot?.windowTitle ?: JSONObject.NULL)
      })
      put("screen", JSONObject().apply {
        put("widthPx", displayInfo.widthPx)
        put("heightPx", displayInfo.heightPx)
        put("rotation", displayInfo.rotation)
        put("densityDpi", displayInfo.densityDpi)
      })
      put("runtime", JSONObject().apply {
        put("projectionReady", projectionReady)
        put("accessibilityReady", accessibilitySnapshot != null)
      })
    }
  }
}
