package com.flowy.explore.blocks

import com.flowy.explore.foundation.RootShellRunner
import com.flowy.explore.runtime.AccessibilitySnapshot
import com.flowy.explore.runtime.AccessibilitySnapshotStore
import com.flowy.explore.support.FakeProcess
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Test

class ScrollBlockTest {
  @Test
  fun run_rootScrollsForwardFromResolvedBounds() {
    AccessibilitySnapshotStore.update(
      AccessibilitySnapshot(
        capturedAt = "2026-04-26T10:00:00+08:00",
        packageName = "com.flowy.explore",
        windowTitle = null,
        rawJson = """{"nodes":[{"text":"feed","boundsInScreen":{"left":0,"top":100,"right":300,"bottom":900},"flags":{"scrollable":true}}]}""",
      ),
    )
    val commands = mutableListOf<List<String>>()
    val block = ScrollBlock(
      RootShellRunner(
        candidates = listOf("su"),
        processFactory = { command ->
          commands += command
          FakeProcess()
        },
      ),
    )

    try {
      val message = block.run(JSONObject("""{"backend":"root","direction":"down","selector":{"textContains":"feed"}}"""))

      assertEquals("scroll:root:down", message)
      assertEquals(listOf("su", "-c", "input swipe 150 740 150 260 250"), commands.single())
    } finally {
      AccessibilitySnapshotStore.clear()
    }
  }

  @Test(expected = IllegalStateException::class)
  fun run_rejectsUnsupportedDirection() {
    AccessibilitySnapshotStore.update(
      AccessibilitySnapshot(
        capturedAt = "2026-04-26T10:00:00+08:00",
        packageName = "com.flowy.explore",
        windowTitle = null,
        rawJson = """{"nodes":[{"text":"feed","boundsInScreen":{"left":0,"top":100,"right":300,"bottom":900},"flags":{"scrollable":true}}]}""",
      ),
    )
    try {
      ScrollBlock(
        RootShellRunner(candidates = listOf("su"), processFactory = { FakeProcess() }),
      ).run(JSONObject("""{"backend":"root","direction":"left","selector":{"textContains":"feed"}}"""))
    } finally {
      AccessibilitySnapshotStore.clear()
    }
  }
}
