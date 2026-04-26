package com.flowy.explore.blocks

import com.flowy.explore.foundation.AccessibilityTargeting
import com.flowy.explore.runtime.AccessibilitySnapshotStore
import com.flowy.explore.runtime.FlowyAccessibilityService
import org.json.JSONObject

class TapBlock {
  fun run(payload: JSONObject): String {
    val point = AccessibilityTargeting.pointFromPayload(payload) ?: resolvePoint(payload)
    check(FlowyAccessibilityService.requireInstance().performTap(point.first, point.second)) { "TAP_FAILED" }
    return "tap:${point.first},${point.second}"
  }

  private fun resolvePoint(payload: JSONObject): Pair<Int, Int> {
    val selector = AccessibilityTargeting.selectorFromPayload(payload)
    val bounds = AccessibilityTargeting.resolveBounds(AccessibilitySnapshotStore.currentJson(), selector)
      ?: error("TARGET_NOT_FOUND")
    return bounds.centerX() to bounds.centerY()
  }
}
