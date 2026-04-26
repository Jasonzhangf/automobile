package com.flowy.explore.blocks

import com.flowy.explore.foundation.DisplayInfo
import com.flowy.explore.foundation.ObservedPageState
import com.flowy.explore.runtime.AccessibilitySnapshot
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Test

class EvaluateAnchorBlockTest {
  @Test
  fun run_marksAnchorMatched() {
    val pageState = ObservedPageState(
      pageContext = JSONObject("""{"app":{"packageName":"com.xhs.app"},"screen":{},"runtime":{"projectionReady":true,"accessibilityReady":true}}"""),
      pageSignature = "sig123",
      displayInfo = DisplayInfo(1216, 2640, 560, 0, 0),
      accessibilitySnapshot = AccessibilitySnapshot(
        capturedAt = "2026-04-26T10:00:00+08:00",
        packageName = "com.xhs.app",
        windowTitle = null,
        rawJson = """{"nodes":[{"text":"搜索","contentDescription":"","hintText":""}]}""",
      ),
      screenshotCapture = null,
    )

    val result = EvaluateAnchorBlock().run(
      pageState,
      JSONObject().apply {
        put("phase", "pre")
        put("pageSignature", "sig123")
        put("packageName", "com.xhs.app")
        put("requireProjectionReady", true)
        put("mustContainTexts", JSONArray().put("搜索"))
      },
    )

    assertEquals("ok", result.getString("status"))
    assertEquals(true, result.getJSONObject("output").getBoolean("matched"))
  }

  @Test
  fun run_rejectsInvalidPhase() {
    val pageState = ObservedPageState(
      pageContext = JSONObject("""{"app":{},"screen":{},"runtime":{}}"""),
      pageSignature = "sig123",
      displayInfo = DisplayInfo(1216, 2640, 560, 0, 0),
      accessibilitySnapshot = null,
      screenshotCapture = null,
    )

    val input = JSONObject()
    input.put("phase", "during")
    val result = EvaluateAnchorBlock().run(pageState, input)

    assertEquals("error", result.getString("status"))
    assertEquals("INVALID_ANCHOR_PHASE", result.getJSONObject("error").getString("code"))
  }
}
