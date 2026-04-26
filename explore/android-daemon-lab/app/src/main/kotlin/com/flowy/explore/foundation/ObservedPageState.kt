package com.flowy.explore.foundation

import com.flowy.explore.runtime.AccessibilitySnapshot
import com.flowy.explore.runtime.ProjectionCapture
import org.json.JSONObject

data class ObservedPageState(
  val pageContext: JSONObject,
  val pageSignature: String,
  val displayInfo: DisplayInfo,
  val accessibilitySnapshot: AccessibilitySnapshot?,
  val screenshotCapture: ProjectionCapture?,
) {
  fun accessibilityJson(): JSONObject? = accessibilitySnapshot?.rawJson?.let(::JSONObject)
}
