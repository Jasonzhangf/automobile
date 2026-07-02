package com.flowy.explore.runtime.adapter

import com.flowy.explore.foundation.executor.AccessibilitySource
import com.flowy.explore.runtime.AccessibilitySnapshotStore
import org.json.JSONObject

object AccessibilitySnapshotAdapter : AccessibilitySource {
  override fun currentJson(): JSONObject? {
    return AccessibilitySnapshotStore.currentJson()
  }
}
