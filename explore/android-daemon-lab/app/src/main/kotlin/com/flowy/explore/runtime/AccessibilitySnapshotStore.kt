package com.flowy.explore.runtime

import org.json.JSONObject

data class AccessibilitySnapshot(
  val capturedAt: String,
  val packageName: String?,
  val windowTitle: String?,
  val rawJson: String,
)

object AccessibilitySnapshotStore {
  @Volatile private var snapshot: AccessibilitySnapshot? = null

  fun update(next: AccessibilitySnapshot) {
    snapshot = next
  }

  fun clear() {
    snapshot = null
  }

  fun current(): AccessibilitySnapshot? = snapshot

  fun currentJson(): JSONObject? = snapshot?.rawJson?.let(::JSONObject)
}
