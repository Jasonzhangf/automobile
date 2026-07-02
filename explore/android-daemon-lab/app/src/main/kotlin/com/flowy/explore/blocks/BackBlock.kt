package com.flowy.explore.blocks

import com.flowy.explore.foundation.executor.InputExecutor
import com.flowy.explore.runtime.adapter.AccessibilityInputAdapter
import org.json.JSONObject

class BackBlock(
  private val inputExecutor: InputExecutor = AccessibilityInputAdapter,
) {
  fun run(payload: JSONObject): String {
    val keyCode = payload.optInt("keyCode", 4)
    check(inputExecutor.pressKey(keyCode)) { "BACK_FAILED" }
    return "back:$keyCode"
  }
}
