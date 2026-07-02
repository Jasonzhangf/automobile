package com.flowy.explore.blocks

import com.flowy.explore.foundation.executor.InputExecutor
import com.flowy.explore.runtime.adapter.AccessibilityInputAdapter
import org.json.JSONObject

class InputTextBlock(
  private val inputExecutor: InputExecutor = AccessibilityInputAdapter,
) {
  fun run(payload: JSONObject): String {
    val text = payload.optString("text", "")
    check(text.isNotBlank()) { "EMPTY_TEXT" }
    check(inputExecutor.inputText(text)) { "INPUT_TEXT_FAILED" }
    return "input-text:${text.length}chars"
  }
}
