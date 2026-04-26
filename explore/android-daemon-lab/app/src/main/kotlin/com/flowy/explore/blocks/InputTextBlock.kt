package com.flowy.explore.blocks

import android.os.Bundle
import android.view.accessibility.AccessibilityNodeInfo
import com.flowy.explore.foundation.AccessibilityTargeting
import com.flowy.explore.runtime.FlowyAccessibilityService
import org.json.JSONObject

class InputTextBlock {
  fun run(payload: JSONObject): String {
    val text = payload.optString("text").ifBlank { error("INPUT_TEXT_MISSING") }
    val selector = AccessibilityTargeting.selectorFromPayload(payload, defaultEditable = true)
    val root = FlowyAccessibilityService.requireInstance().rootInActiveWindow ?: error("ACCESSIBILITY_ROOT_UNAVAILABLE")
    val node = AccessibilityTargeting.findFirstNode(root, selector) ?: run {
      root.recycle()
      error("INPUT_TARGET_NOT_FOUND")
    }
    try {
      check(node.isFocused || node.performAction(AccessibilityNodeInfo.ACTION_FOCUS)) { "INPUT_FOCUS_FAILED" }
      val args = Bundle().apply {
        putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text)
      }
      check(node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)) { "INPUT_SET_TEXT_FAILED" }
      return "input:${text.length}"
    } finally {
      if (node !== root) root.recycle()
      node.recycle()
    }
  }
}
