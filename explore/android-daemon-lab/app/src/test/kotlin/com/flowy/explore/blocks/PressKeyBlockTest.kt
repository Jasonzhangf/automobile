package com.flowy.explore.blocks

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

  @Test(expected = IllegalStateException::class)
  fun globalActionFor_rejectsUnsupportedKey() {
    block.globalActionFor("enter")
  }
}
