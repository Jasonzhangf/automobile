package com.flowy.explore.blocks

import com.flowy.explore.foundation.RootShellRunner
import com.flowy.explore.runtime.AccessibilitySnapshot
import com.flowy.explore.runtime.AccessibilitySnapshotStore
import com.flowy.explore.support.FakeProcess
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Test

class TapBlockTest {
  @Test
  fun run_rootUsesExplicitPoint() {
    val commands = mutableListOf<List<String>>()
    val block = TapBlock(
      RootShellRunner(
        candidates = listOf("su"),
        processFactory = { command ->
          commands += command
          FakeProcess()
        },
      ),
    )

    val message = block.run(JSONObject("""{"backend":"root","x":12,"y":34}"""))

    assertEquals("tap:root:12,34", message)
    assertEquals(listOf("su", "-c", "input tap 12 34"), commands.single())
  }

  @Test
  fun run_rootResolvesPointFromSnapshotSelector() {
    AccessibilitySnapshotStore.update(
      AccessibilitySnapshot(
        capturedAt = "2026-04-26T10:00:00+08:00",
        packageName = "com.flowy.explore",
        windowTitle = null,
        rawJson = """{"nodes":[{"text":"START","boundsInScreen":{"left":10,"top":20,"right":110,"bottom":220},"flags":{"clickable":true}}]}""",
      ),
    )
    val commands = mutableListOf<List<String>>()
    val block = TapBlock(
      RootShellRunner(
        candidates = listOf("su"),
        processFactory = { command ->
          commands += command
          FakeProcess()
        },
      ),
    )

    try {
      val message = block.run(JSONObject("""{"backend":"root","selector":{"textContains":"START"}}"""))

      assertEquals("tap:root:60,120", message)
      assertEquals(listOf("su", "-c", "input tap 60 120"), commands.single())
    } finally {
      AccessibilitySnapshotStore.clear()
    }
  }

  @Test(expected = IllegalStateException::class)
  fun run_rejectsMissingTarget() {
    AccessibilitySnapshotStore.clear()
    TapBlock(
      RootShellRunner(candidates = listOf("su"), processFactory = { FakeProcess() }),
    ).run(JSONObject("""{"backend":"root","selector":{"textContains":"missing"}}"""))
  }
}
