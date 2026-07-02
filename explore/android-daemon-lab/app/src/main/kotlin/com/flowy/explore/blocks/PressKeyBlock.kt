package com.flowy.explore.blocks

import com.flowy.explore.foundation.executor.InputExecutor
import com.flowy.explore.runtime.adapter.AccessibilityInputAdapter
import org.json.JSONObject

class PressKeyBlock(
  private val inputExecutor: InputExecutor = AccessibilityInputAdapter,
) {
  fun run(payload: JSONObject): String {
    val keyCode = payload.optInt("keyCode", 0)
    check(keyCode > 0) { "INVALID_KEY_CODE" }
    check(inputExecutor.pressKey(keyCode)) { "PRESS_KEY_FAILED" }
    return "press-key:$keyCode"
  }
}
