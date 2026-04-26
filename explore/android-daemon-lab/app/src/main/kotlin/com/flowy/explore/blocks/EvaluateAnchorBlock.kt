package com.flowy.explore.blocks

import com.flowy.explore.foundation.AccessibilityTargeting
import com.flowy.explore.foundation.BlockResultFactory
import com.flowy.explore.foundation.ObservedPageState
import com.flowy.explore.foundation.TimeHelper
import org.json.JSONArray
import org.json.JSONObject

class EvaluateAnchorBlock(
  private val now: () -> String = TimeHelper::now,
) {
  fun run(pageState: ObservedPageState, input: JSONObject): JSONObject {
    val startedAt = now()
    return try {
      val phase = input.optString("phase").ifBlank { "pre" }
      if (phase != "pre" && phase != "post") error("INVALID_ANCHOR_PHASE")
      val anchor = input.optJSONObject("anchor") ?: input
      val reasons = JSONArray()
      var matched = true
      matched = matched and checkPageSignature(pageState, anchor, reasons)
      matched = matched and checkPackageName(pageState, anchor, reasons)
      matched = matched and checkProjectionReady(pageState, anchor, reasons)
      matched = matched and checkTexts(pageState, anchor, reasons)
      if (reasons.length() == 0) reasons.put("no constraints")
      BlockResultFactory.ok(
        startedAt = startedAt,
        output = JSONObject().apply {
          put("matched", matched)
          put("phase", phase)
          put("reasons", reasons)
        },
      )
    } catch (throwable: Throwable) {
      val code = throwable.message ?: "ANCHOR_EVALUATION_FAILED"
      BlockResultFactory.error(startedAt = startedAt, code = code, message = code)
    }
  }

  private fun checkPageSignature(pageState: ObservedPageState, anchor: JSONObject, reasons: JSONArray): Boolean {
    val expected = anchor.optString("pageSignature", "").ifBlank { return true }
    return if (pageState.pageSignature == expected) {
      reasons.put("pageSignature matched")
      true
    } else {
      reasons.put("pageSignature mismatch")
      false
    }
  }

  private fun checkPackageName(pageState: ObservedPageState, anchor: JSONObject, reasons: JSONArray): Boolean {
    val expected = anchor.optString("packageName", "").ifBlank { return true }
    val actual = pageState.pageContext.optJSONObject("app")?.optString("packageName", "")
    return if (actual == expected) {
      reasons.put("packageName matched")
      true
    } else {
      reasons.put("packageName mismatch")
      false
    }
  }

  private fun checkProjectionReady(pageState: ObservedPageState, anchor: JSONObject, reasons: JSONArray): Boolean {
    if (!anchor.has("requireProjectionReady")) return true
    val expected = anchor.getBoolean("requireProjectionReady")
    val actual = pageState.pageContext.optJSONObject("runtime")?.optBoolean("projectionReady") == true
    return if (actual == expected) {
      reasons.put("projectionReady matched")
      true
    } else {
      reasons.put("projectionReady mismatch")
      false
    }
  }

  private fun checkTexts(pageState: ObservedPageState, anchor: JSONObject, reasons: JSONArray): Boolean {
    val texts = anchor.optJSONArray("mustContainTexts") ?: return true
    var matched = true
    for (index in 0 until texts.length()) {
      val text = texts.optString(index)
      if (text.isBlank()) continue
      if (AccessibilityTargeting.textExists(pageState.accessibilityJson(), text)) {
        reasons.put("text matched: $text")
      } else {
        reasons.put("text missing: $text")
        matched = false
      }
    }
    return matched
  }
}
