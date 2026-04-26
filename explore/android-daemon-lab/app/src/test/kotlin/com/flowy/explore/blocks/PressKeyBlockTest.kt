package com.flowy.explore.blocks

import com.flowy.explore.foundation.RootShellRunner
import com.flowy.explore.support.FakeProcess
import android.accessibilityservice.AccessibilityService
import org.junit.Assert.assertEquals
import org.junit.Test

class PressKeyBlockTest {
  private val block = PressKeyBlock()

  @Test
  fun globalActionFor_mapsKnownKeys() {
    assertEquals(AccessibilityService.GLOBAL_ACTION_BACK, block.globalActionFor("back"))
    assertEquals(AccessibilityService.GLOBAL_ACTION_HOME, block.globalActionFor("home"))
  }

  @Test
  fun rootCommandFor_mapsKnownKeys() {
    assertEquals("input keyevent 4", block.rootCommandFor("back"))
    assertEquals("input keyevent 3", block.rootCommandFor("home"))
    assertEquals("cmd statusbar expand-notifications", block.rootCommandFor("notifications"))
  }

  @Test
  fun run_rootExecutesResolvedCommand() {
    val commands = mutableListOf<List<String>>()
    val block = PressKeyBlock(
      RootShellRunner(
        candidates = listOf("su"),
        processFactory = { command ->
          commands += command
          FakeProcess()
        },
      ),
    )

    val message = block.run(org.json.JSONObject("""{"backend":"root","key":"home"}"""))

    assertEquals("press-key:root:home", message)
    assertEquals(listOf("su", "-c", "input keyevent 3"), commands.single())
  }

  @Test(expected = IllegalStateException::class)
  fun globalActionFor_rejectsUnsupportedKey() {
    block.globalActionFor("enter")
  }

  @Test(expected = IllegalStateException::class)
  fun rootCommandFor_rejectsUnsupportedKey() {
    block.rootCommandFor("enter")
  }
}
