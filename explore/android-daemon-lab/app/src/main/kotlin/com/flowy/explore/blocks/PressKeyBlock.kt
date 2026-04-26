package com.flowy.explore.blocks

import android.accessibilityservice.AccessibilityService
import com.flowy.explore.runtime.FlowyAccessibilityService
import org.json.JSONObject

class PressKeyBlock {
  fun run(payload: JSONObject): String {
    val key = payload.optString("key").ifBlank { error("KEY_MISSING") }
    val action = globalActionFor(key)
    check(FlowyAccessibilityService.requireInstance().performGlobal(action)) { "PRESS_KEY_FAILED" }
    return "press-key:$key"
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
}
