package com.flowy.explore.foundation

import org.json.JSONObject

/**
 * Minimal page state snapshot. No longer depends on runtime.AccessibilitySnapshot or ProjectionCapture.
 * Blocks only need nodes + rawJson; flows that need more compose their own metadata.
 */
data class ObservedPageState(
  val nodes: List<JSONObject>,
  val rawJson: JSONObject,
) {
  /** Compatibility alias for flows that still call this method. */
  val pageContext: JSONObject
    get() = rawJson
}
