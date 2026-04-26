package com.flowy.explore.blocks

import com.flowy.explore.foundation.AccessibilityTargeting
import com.flowy.explore.runtime.AccessibilitySnapshotStore
import com.flowy.explore.runtime.FlowyAccessibilityService
import org.json.JSONObject
import kotlin.math.max
import kotlin.math.min

class ScrollBlock {
  fun run(payload: JSONObject): String {
    val selector = AccessibilityTargeting.selectorFromPayload(payload, defaultScrollable = true)
    val bounds = AccessibilityTargeting.resolveBounds(AccessibilitySnapshotStore.currentJson(), selector)
      ?: error("SCROLL_TARGET_NOT_FOUND")
    val direction = payload.optString("direction").ifBlank { "forward" }.lowercase()
    val points = when (direction) {
      "forward", "down" -> listOf(
        bounds.centerX(),
        min(bounds.bottom - 24, bounds.top + ((bounds.bottom - bounds.top) * 8 / 10)),
        bounds.centerX(),
        max(bounds.top + 24, bounds.top + ((bounds.bottom - bounds.top) * 2 / 10)),
      )
      "backward", "up" -> listOf(
        bounds.centerX(),
        max(bounds.top + 24, bounds.top + ((bounds.bottom - bounds.top) * 2 / 10)),
        bounds.centerX(),
        min(bounds.bottom - 24, bounds.top + ((bounds.bottom - bounds.top) * 8 / 10)),
      )
      else -> error("UNSUPPORTED_SCROLL_DIRECTION")
    }
    check(FlowyAccessibilityService.requireInstance().performSwipe(points[0], points[1], points[2], points[3])) {
      "SCROLL_FAILED"
    }
    return "scroll:$direction"
  }
}
