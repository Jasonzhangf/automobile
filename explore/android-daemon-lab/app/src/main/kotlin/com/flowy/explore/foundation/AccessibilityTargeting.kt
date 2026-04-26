package com.flowy.explore.foundation

import android.graphics.Rect
import android.view.accessibility.AccessibilityNodeInfo
import org.json.JSONObject

data class TargetBounds(
  val left: Int,
  val top: Int,
  val right: Int,
  val bottom: Int,
) {
  fun centerX(): Int = (left + right) / 2
  fun centerY(): Int = (top + bottom) / 2
}

data class TargetSelector(
  val textContains: String? = null,
  val contentDescContains: String? = null,
  val hintTextContains: String? = null,
  val editable: Boolean? = null,
  val scrollable: Boolean? = null,
  val clickable: Boolean? = null,
  val bounds: TargetBounds? = null,
)

object AccessibilityTargeting {
  fun findMatchingNodes(snapshot: JSONObject?, selector: TargetSelector): List<JSONObject> {
    val nodes = snapshot?.optJSONArray("nodes") ?: return emptyList()
    val matched = mutableListOf<JSONObject>()
    for (index in 0 until nodes.length()) {
      val node = nodes.optJSONObject(index) ?: continue
      if (matches(node, selector)) matched += node
    }
    return matched
  }

  fun pointFromPayload(payload: JSONObject): Pair<Int, Int>? {
    if (!payload.has("x") || !payload.has("y")) return null
    return payload.getInt("x") to payload.getInt("y")
  }

  fun selectorFromPayload(
    payload: JSONObject,
    defaultEditable: Boolean = false,
    defaultScrollable: Boolean = false,
  ): TargetSelector {
    val selectorJson = payload.optJSONObject("selector") ?: JSONObject()
    return TargetSelector(
      textContains = selectorJson.optString("textContains", "").ifBlank { null },
      contentDescContains = selectorJson.optString("contentDescContains", "").ifBlank { null },
      hintTextContains = selectorJson.optString("hintTextContains", "").ifBlank { null },
      editable = selectorJson.optBooleanOrNull("editable") ?: defaultEditable.takeIf { it },
      scrollable = selectorJson.optBooleanOrNull("scrollable") ?: defaultScrollable.takeIf { it },
      clickable = selectorJson.optBooleanOrNull("clickable"),
      bounds = payload.optBounds() ?: selectorJson.optBounds(),
    )
  }

  fun resolveBounds(snapshot: JSONObject?, selector: TargetSelector): TargetBounds? {
    if (selector.bounds != null) return selector.bounds
    return findMatchingNodes(snapshot, selector).firstOrNull()?.optBoundsInScreen()
  }

  fun textExists(snapshot: JSONObject?, expected: String): Boolean {
    if (expected.isBlank()) return true
    val nodes = snapshot?.optJSONArray("nodes") ?: return false
    for (index in 0 until nodes.length()) {
      val node = nodes.optJSONObject(index) ?: continue
      val values = listOf(
        node.optString("text", ""),
        node.optString("contentDescription", ""),
        node.optString("hintText", ""),
      )
      if (values.any { it.contains(expected, ignoreCase = true) }) return true
    }
    return false
  }

  fun findFirstNode(root: AccessibilityNodeInfo?, selector: TargetSelector): AccessibilityNodeInfo? {
    if (root == null) return null
    if (matches(root, selector)) return root
    for (index in 0 until root.childCount) {
      val child = root.getChild(index) ?: continue
      val matched = findFirstNode(child, selector)
      if (matched != null) return matched
      child.recycle()
    }
    return null
  }

  private fun matches(node: JSONObject, selector: TargetSelector): Boolean {
    val flags = node.optJSONObject("flags") ?: JSONObject()
    return selector.textContains.matches(node.optString("text", "")) &&
      selector.contentDescContains.matches(node.optString("contentDescription", "")) &&
      selector.hintTextContains.matches(node.optString("hintText", "")) &&
      selector.editable.matches(flags.optBoolean("editable")) &&
      selector.scrollable.matches(flags.optBoolean("scrollable")) &&
      selector.clickable.matches(flags.optBoolean("clickable")) &&
      selector.bounds.matches(node.optBoundsInScreen())
  }

  private fun matches(node: AccessibilityNodeInfo, selector: TargetSelector): Boolean {
    return selector.textContains.matches(node.text?.toString()) &&
      selector.contentDescContains.matches(node.contentDescription?.toString()) &&
      selector.hintTextContains.matches(node.hintText?.toString()) &&
      selector.editable.matches(node.isEditable) &&
      selector.scrollable.matches(node.isScrollable) &&
      selector.clickable.matches(node.isClickable) &&
      selector.bounds.matches(node.boundsInScreen())
  }

  private fun AccessibilityNodeInfo.boundsInScreen(): TargetBounds {
    val rect = Rect()
    getBoundsInScreen(rect)
    return TargetBounds(rect.left, rect.top, rect.right, rect.bottom)
  }

  private fun JSONObject.optBoundsInScreen(): TargetBounds? {
    val bounds = optJSONObject("boundsInScreen") ?: return null
    return TargetBounds(bounds.optInt("left"), bounds.optInt("top"), bounds.optInt("right"), bounds.optInt("bottom"))
  }

  private fun JSONObject.optBounds(): TargetBounds? {
    val bounds = optJSONObject("bounds") ?: return null
    return TargetBounds(bounds.optInt("left"), bounds.optInt("top"), bounds.optInt("right"), bounds.optInt("bottom"))
  }

  private fun JSONObject.optBooleanOrNull(key: String): Boolean? = if (has(key)) getBoolean(key) else null

  private fun String?.matches(value: String?): Boolean = this == null || value?.contains(this, ignoreCase = true) == true

  private fun Boolean?.matches(value: Boolean): Boolean = this == null || this == value

  private fun TargetBounds?.matches(value: TargetBounds?): Boolean = this == null || value == this
}
