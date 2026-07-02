package com.flowy.explore.blocks

import com.flowy.explore.foundation.executor.AccessibilitySource
import com.flowy.explore.runtime.adapter.AccessibilitySnapshotAdapter

/**
 * 通过 AccessibilitySource 接口获取当前 Accessibility 快照。
 * 不再直接依赖 AccessibilitySnapshotStore（已被 adapter 封装）。
 */
class DumpAccessibilityTreeBlock(
  private val source: AccessibilitySource = AccessibilitySnapshotAdapter,
) {
  fun run(): org.json.JSONObject? {
    return source.currentJson()
  }
}
