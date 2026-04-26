package com.flowy.explore.blocks

import com.flowy.explore.foundation.DisplayInfo
import com.flowy.explore.runtime.AccessibilitySnapshot
import com.flowy.explore.runtime.ProjectionCapture
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ObservePageBlockTest {
  @Test
  fun observe_buildsPageStateFromAccessibility() {
    val snapshot = AccessibilitySnapshot(
      capturedAt = "2026-04-26T10:00:00+08:00",
      packageName = "com.xhs.app",
      windowTitle = "搜索",
      rawJson = """{"nodes":[{"text":"搜索","contentDescription":"","hintText":""}]}""",
    )
    val block = ObservePageBlock(
      readDisplayInfo = { DisplayInfo(1216, 2640, 560, 0, 0) },
      dumpAccessibility = { snapshot },
      dumpRootUi = { null },
      peekAccessibility = { snapshot },
      captureScreenshot = { ProjectionCapture(byteArrayOf(1), DisplayInfo(1216, 2640, 560, 0, 0)) },
      isProjectionReady = { false },
      now = { "2026-04-26T10:00:01+08:00" },
    )

    val observerSpec = JSONObject()
    observerSpec.put("requireAccessibility", true)
    val observed = block.observe("req1", "run1", "dump-accessibility-tree", observerSpec)

    assertEquals("com.xhs.app", observed.pageContext.getJSONObject("app").getString("packageName"))
    assertEquals(true, observed.pageContext.getJSONObject("runtime").getBoolean("accessibilityReady"))
    assertTrue(observed.pageSignature.isNotBlank())
  }

  @Test
  fun run_returnsStandardSuccessResult() {
    val snapshot = AccessibilitySnapshot(
      capturedAt = "2026-04-26T10:00:00+08:00",
      packageName = "com.flowy.explore",
      windowTitle = null,
      rawJson = """{"nodes":[]}""",
    )
    val block = ObservePageBlock(
      readDisplayInfo = { DisplayInfo(1216, 2640, 560, 0, 0) },
      dumpAccessibility = { snapshot },
      dumpRootUi = { null },
      peekAccessibility = { snapshot },
      captureScreenshot = { ProjectionCapture(byteArrayOf(1), DisplayInfo(1216, 2640, 560, 0, 0)) },
      isProjectionReady = { true },
      now = { "2026-04-26T10:00:01+08:00" },
    )

    val observerSpec = JSONObject()
    observerSpec.put("requireAccessibility", true)
    val result = block.run("req1", "run1", "observe-page", observerSpec)

    assertEquals("ok", result.getString("status"))
    assertEquals("page-state:current", result.getJSONObject("output").getString("pageStateRef"))
  }
}
