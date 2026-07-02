package com.flowy.explore.foundation.executor

import org.json.JSONObject

/**
 * Abstraction over accessibility tree source.
 * Allows blocks to depend on the interface instead of the runtime snapshot store.
 */
interface AccessibilitySource {
  fun currentJson(): JSONObject?
}
