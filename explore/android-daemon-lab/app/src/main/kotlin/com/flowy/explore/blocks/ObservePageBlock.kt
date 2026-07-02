package com.flowy.explore.blocks

import com.flowy.explore.foundation.AccessibilityTargeting
import com.flowy.explore.foundation.ObservedPageState
import com.flowy.explore.foundation.executor.AccessibilitySource
import com.flowy.explore.runtime.adapter.AccessibilitySnapshotAdapter
import org.json.JSONObject

/**
 * ObservePageBlock retrieves the current page state through the AccessibilitySource interface.
 * Blocks no longer depend on the runtime snapshot store directly.
 */
class ObservePageBlock(
  private val snapshotSource: AccessibilitySource = AccessibilitySnapshotAdapter,
) {
  fun run(): ObservedPageState {
    val json = snapshotSource.currentJson() ?: error("NO_ACCESSIBILITY_SNAPSHOT")
    val nodes = ExtractionHelper.extractNodes(json)
    return ObservedPageState(nodes = nodes, rawJson = json)
  }
}

internal object ExtractionHelper {
  fun extractNodes(json: JSONObject): List<JSONObject> {
    val result = mutableListOf<JSONObject>()
    val jsonNodes = json.optJSONArray("nodes")
    if (jsonNodes != null) {
      for (i in 0 until jsonNodes.length()) {
        result.add(jsonNodes.getJSONObject(i))
      }
    }
    return result
  }
}
