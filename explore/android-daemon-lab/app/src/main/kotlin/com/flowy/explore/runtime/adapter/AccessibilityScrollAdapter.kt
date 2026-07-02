package com.flowy.explore.runtime.adapter

import com.flowy.explore.foundation.executor.ScrollExecutor
import com.flowy.explore.runtime.FlowyAccessibilityService

object AccessibilityScrollAdapter : ScrollExecutor {
  override fun scroll(x: Int, y: Int, direction: String): Boolean {
    val svc = FlowyAccessibilityService.requireInstance()
    return svc.performScroll(x, y, direction)
  }
}
