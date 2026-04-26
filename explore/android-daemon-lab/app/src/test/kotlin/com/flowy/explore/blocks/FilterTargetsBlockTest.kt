package com.flowy.explore.blocks

import com.flowy.explore.foundation.DisplayInfo
import com.flowy.explore.foundation.ObservedPageState
import com.flowy.explore.runtime.AccessibilitySnapshot
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Test

class FilterTargetsBlockTest {
  @Test
  fun run_returnsMatchedTargets() {
    val pageState = ObservedPageState(
      pageContext = JSONObject("""{"app":{"packageName":"com.xhs.app"},"screen":{"widthPx":1216},"runtime":{"projectionReady":true,"accessibilityReady":true}}"""),
      pageSignature = "abc123",
      displayInfo = DisplayInfo(1216, 2640, 560, 0, 0),
      accessibilitySnapshot = AccessibilitySnapshot(
        capturedAt = "2026-04-26T10:00:00+08:00",
        packageName = "com.xhs.app",
        windowTitle = null,
        rawJson = """{"nodes":[{"text":"搜索","contentDescription":"","hintText":"","className":"android.widget.TextView","boundsInScreen":{"left":1,"top":2,"right":3,"bottom":4},"flags":{"clickable":true}}]}""",
      ),
      screenshotCapture = null,
    )

    val result = FilterTargetsBlock().run(
      pageState,
      JSONObject("""{"selector":{"textContains":"搜索"}}"""),
    )

    assertEquals("ok", result.getString("status"))
    assertEquals(1, result.getJSONObject("output").getInt("targetCount"))
    assertEquals("target:auto:0", result.getJSONObject("output").getString("selectedTargetRef"))
  }

  @Test
  fun run_returnsErrorWhenSelectorMissing() {
    val pageState = ObservedPageState(
      pageContext = JSONObject("""{"app":{},"screen":{},"runtime":{}}"""),
      pageSignature = "abc123",
      displayInfo = DisplayInfo(1216, 2640, 560, 0, 0),
      accessibilitySnapshot = null,
      screenshotCapture = null,
    )

    val result = FilterTargetsBlock().run(pageState, JSONObject())

    assertEquals("error", result.getString("status"))
    assertEquals("FILTER_SELECTOR_MISSING", result.getJSONObject("error").getString("code"))
  }
}
