package com.flowy.explore.foundation

import org.junit.Assert.assertEquals
import org.junit.Test

class AccessibilityTargetingTest {
  @Test
  fun targetBounds_centerUsesMidpoint() {
    val bounds = TargetBounds(left = 20, top = 40, right = 220, bottom = 140)
    assertEquals(120, bounds.centerX())
    assertEquals(90, bounds.centerY())
  }

  @Test
  fun targetSelector_storesExplicitFlags() {
    val selector = TargetSelector(textContains = "搜索", editable = true, clickable = true)
    assertEquals("搜索", selector.textContains)
    assertEquals(true, selector.editable)
    assertEquals(true, selector.clickable)
  }
}
