package com.flowy.explore.blocks

import com.flowy.explore.foundation.executor.ScrollExecutor
import com.flowy.explore.runtime.adapter.AccessibilityScrollAdapter
import org.json.JSONObject

class ScrollBlock(
  private val scrollExecutor: ScrollExecutor = AccessibilityScrollAdapter,
) {
  fun run(payload: JSONObject): String {
    val x = payload.optInt("x", 540)
    val y = payload.optInt("y", 1200)
    val direction = payload.optString("direction", "forward")
    check(scrollExecutor.scroll(x, y, direction)) { "SCROLL_FAILED" }
    return "scroll:$direction:$x,$y"
  }
}
