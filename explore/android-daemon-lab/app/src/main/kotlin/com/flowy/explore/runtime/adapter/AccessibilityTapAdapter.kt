package com.flowy.explore.runtime.adapter

import com.flowy.explore.foundation.executor.TapExecutor
import com.flowy.explore.runtime.FlowyAccessibilityService

object AccessibilityTapAdapter : TapExecutor {
  override fun tap(x: Int, y: Int): Boolean {
    return FlowyAccessibilityService.requireInstance().performTap(x, y)
  }
}
