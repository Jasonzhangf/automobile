package com.flowy.explore.blocks

import android.accessibilityservice.AccessibilityService
import com.flowy.explore.foundation.OperationBackend
import com.flowy.explore.foundation.RootShellRunner
import com.flowy.explore.runtime.FlowyAccessibilityService
import org.json.JSONObject

class PressKeyBlock(
  private val rootShellRunner: RootShellRunner = RootShellRunner(),
) {
  fun run(payload: JSONObject): String {
    val key = payload.optString("key").ifBlank { error("KEY_MISSING") }
    return when (OperationBackend.fromPayload(payload)) {
      OperationBackend.ACCESSIBILITY -> {
        val action = globalActionFor(key)
        check(FlowyAccessibilityService.requireInstance().performGlobal(action)) { "PRESS_KEY_FAILED" }
        "press-key:accessibility:$key"
      }
      OperationBackend.ROOT -> {
        val result = rootShellRunner.run(rootCommandFor(key))
        check(result.exitCode == 0) { "PRESS_KEY_FAILED" }
        "press-key:root:$key"
      }
    }
  }

  fun globalActionFor(key: String): Int {
    return when (key.lowercase()) {
      "back" -> AccessibilityService.GLOBAL_ACTION_BACK
      "home" -> AccessibilityService.GLOBAL_ACTION_HOME
      "recents", "recent_apps" -> AccessibilityService.GLOBAL_ACTION_RECENTS
      "notifications" -> AccessibilityService.GLOBAL_ACTION_NOTIFICATIONS
      "quick_settings" -> AccessibilityService.GLOBAL_ACTION_QUICK_SETTINGS
      else -> error("UNSUPPORTED_KEY")
    }
  }

  fun rootCommandFor(key: String): String {
    return when (key.lowercase()) {
      "back" -> "input keyevent 4"
      "home" -> "input keyevent 3"
      "recents", "recent_apps" -> "input keyevent 187"
      "notifications" -> "cmd statusbar expand-notifications"
      "quick_settings" -> "cmd statusbar expand-settings"
      else -> error("UNSUPPORTED_KEY")
    }
  }
}
