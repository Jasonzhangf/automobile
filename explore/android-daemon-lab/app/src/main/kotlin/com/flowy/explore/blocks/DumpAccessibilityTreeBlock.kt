package com.flowy.explore.blocks

import com.flowy.explore.runtime.AccessibilitySnapshot
import com.flowy.explore.runtime.AccessibilitySnapshotStore

class DumpAccessibilityTreeBlock {
  fun run(): AccessibilitySnapshot {
    return AccessibilitySnapshotStore.current()
      ?: error("ACCESSIBILITY_ROOT_UNAVAILABLE")
  }
}
