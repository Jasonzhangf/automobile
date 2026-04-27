package com.flowy.explore.blocks

import android.os.Bundle
import android.view.accessibility.AccessibilityNodeInfo
import com.flowy.explore.foundation.AccessibilityTargeting
import com.flowy.explore.foundation.OperationBackend
import com.flowy.explore.foundation.RootShellRunner
import com.flowy.explore.runtime.FlowyAccessibilityService
import org.json.JSONObject

class InputTextBlock(
  private val rootShellRunner: RootShellRunner = RootShellRunner(),
  private val setClipboard: ((String) -> Boolean)? = null,
) {
  fun run(payload: JSONObject): String {
    val text = payload.optString("text").ifBlank { error("INPUT_TEXT_MISSING") }
    return when (OperationBackend.fromPayload(payload)) {
      OperationBackend.ACCESSIBILITY -> runAccessibility(payload, text)
      OperationBackend.ROOT -> runRoot(text)
    }
  }

  private fun runAccessibility(payload: JSONObject, text: String): String {
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
      return "input:accessibility:${text.length}"
    } finally {
      if (node !== root) root.recycle()
      node.recycle()
    }
  }

  private fun runRoot(text: String): String {
    if (text.isPureAscii()) {
      val normalized = text.replace(" ", "%s")
      val result = rootShellRunner.run("input text $normalized")
      check(result.exitCode == 0) { "INPUT_TEXT_FAILED" }
      return "input:root:${text.length}"
    }
    // Unicode: set clipboard RIGHT before paste to avoid system pollution
    val clipFn = setClipboard ?: error("CLIPBOARD_REQUIRED_FOR_UNICODE")
    check(clipFn(text)) { "CLIPBOARD_SET_FAILED" }
    val pasteResult = rootShellRunner.run("input keyevent 279", 3000)
    check(pasteResult.exitCode == 0) { "PASTE_FAILED" }
    return "input:root:clipboard:${text.length}"
  }

  private fun String.isPureAscii(): Boolean = all { it.code in 0x20..0x7E }
}
