package com.flowy.explore.blocks

import com.flowy.explore.foundation.AccessibilityTargeting
import com.flowy.explore.foundation.OperationBackend
import com.flowy.explore.foundation.RootShellRunner
import com.flowy.explore.runtime.AccessibilitySnapshotStore
import com.flowy.explore.runtime.FlowyAccessibilityService
import org.json.JSONObject
import kotlin.math.max
import kotlin.math.min

class ScrollBlock(
  private val rootShellRunner: RootShellRunner = RootShellRunner(),
) {
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
    return when (OperationBackend.fromPayload(payload)) {
      OperationBackend.ACCESSIBILITY -> {
        check(FlowyAccessibilityService.requireInstance().performSwipe(points[0], points[1], points[2], points[3])) {
          "SCROLL_FAILED"
        }
        "scroll:accessibility:$direction"
      }
      OperationBackend.ROOT -> {
        val result = rootShellRunner.run("input swipe ${points[0]} ${points[1]} ${points[2]} ${points[3]} 250")
        check(result.exitCode == 0) { "SCROLL_FAILED" }
        "scroll:root:$direction"
      }
    }
  }
}
