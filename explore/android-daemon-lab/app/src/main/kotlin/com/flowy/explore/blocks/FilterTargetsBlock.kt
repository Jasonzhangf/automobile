package com.flowy.explore.blocks

import com.flowy.explore.foundation.AccessibilityTargeting
import com.flowy.explore.foundation.BlockResultFactory
import com.flowy.explore.foundation.ObservedPageState
import com.flowy.explore.foundation.TargetSelector
import com.flowy.explore.foundation.TimeHelper
import org.json.JSONArray
import org.json.JSONObject

class FilterTargetsBlock(
  private val now: () -> String = TimeHelper::now,
) {
  fun run(pageState: ObservedPageState, input: JSONObject): JSONObject {
    val startedAt = now()
    return try {
      val selector = selectorFromInput(input)
      if (!selector.hasConstraints()) error("FILTER_SELECTOR_MISSING")
      val nodes = AccessibilityTargeting.findMatchingNodes(pageState.accessibilityJson(), selector)
      if (nodes.isEmpty()) error("TARGET_NOT_FOUND")
      val selectionPolicy = input.optJSONObject("selectionPolicy") ?: input.optJSONObject("selection")
      val selectedIndex = resolveSelectedIndex(nodes.size, selectionPolicy)
      val targetRefs = JSONArray()
      val targets = JSONArray()
      nodes.forEachIndexed { index, node ->
        val ref = "target:auto:$index"
        targetRefs.put(ref)
        targets.put(JSONObject().apply {
          put("targetRef", ref)
          put("text", node.opt("text"))
          put("contentDescription", node.opt("contentDescription"))
          put("className", node.opt("className"))
          put("bounds", node.optJSONObject("boundsInScreen"))
        })
      }
      BlockResultFactory.ok(
        startedAt = startedAt,
        output = JSONObject().apply {
          put("targetCount", nodes.size)
          put("selectedTargetRef", targetRefs.getString(selectedIndex))
          put("targetRefs", targetRefs)
          put("targets", targets)
        },
      )
    } catch (throwable: Throwable) {
      val code = throwable.message ?: "FILTER_TARGETS_FAILED"
      BlockResultFactory.error(startedAt = startedAt, code = code, message = code)
    }
  }

  private fun selectorFromInput(input: JSONObject): TargetSelector {
    val filterJson = input.optJSONObject("filter") ?: JSONObject()
    val selectorJson = input.optJSONObject("selector") ?: filterJson.optJSONObject("selector")
    val payload = JSONObject().apply {
      if (selectorJson != null) put("selector", selectorJson)
      if (input.has("bounds")) put("bounds", input.getJSONObject("bounds"))
    }
    return AccessibilityTargeting.selectorFromPayload(payload)
  }

  private fun TargetSelector.hasConstraints(): Boolean {
    return textContains != null ||
      contentDescContains != null ||
      hintTextContains != null ||
      classNameContains != null ||
      editable != null ||
      scrollable != null ||
      clickable != null ||
      longClickable != null ||
      bounds != null
  }

  private fun resolveSelectedIndex(count: Int, policy: JSONObject?): Int {
    if (policy == null || count == 0) return 0
    val mode = policy.optString("mode", "first")
    return when (mode) {
      "first" -> 0
      "last" -> count - 1
      "nth" -> policy.optInt("index", 0).coerceIn(0, count - 1)
      else -> 0
    }
  }
}
