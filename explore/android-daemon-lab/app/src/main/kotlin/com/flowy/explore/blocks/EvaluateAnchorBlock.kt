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
      matched = matched and checkPackageName(pageState, anchor, reasons)
      matched = matched and checkTexts(pageState, anchor, reasons)
      matched = matched and checkContentDescs(pageState, anchor, reasons)
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

  private fun checkPackageName(pageState: ObservedPageState, anchor: JSONObject, reasons: JSONArray): Boolean {
    val expected = anchor.optString("packageName", "").ifBlank { return true }
    val pkg = pageState.rawJson.optString("packageName", "")
    return if (pkg == expected) {
      reasons.put("packageName matched")
      true
    } else {
      reasons.put("packageName mismatch")
      false
    }
  }

  private fun checkTexts(pageState: ObservedPageState, anchor: JSONObject, reasons: JSONArray): Boolean {
    val texts = anchor.optJSONArray("mustContainTexts") ?: return true
    var matched = true
    for (index in 0 until texts.length()) {
      val text = texts.optString(index)
      if (text.isBlank()) continue
      if (AccessibilityTargeting.textExists(pageState.rawJson, text)) {
        reasons.put("text matched: $text")
      } else {
        reasons.put("text missing: $text")
        matched = false
      }
    }
    return matched
  }

  private fun checkContentDescs(pageState: ObservedPageState, anchor: JSONObject, reasons: JSONArray): Boolean {
    val descs = anchor.optJSONArray("mustContainContentDescs") ?: return true
    var matched = true
    val nodes = pageState.rawJson.optJSONArray("nodes")
    for (index in 0 until descs.length()) {
      val expected = descs.optString(index)
      if (expected.isBlank()) continue
      var found = false
      if (nodes != null) {
        for (n in 0 until nodes.length()) {
          val node = nodes.optJSONObject(n) ?: continue
          val cd = node.optString("contentDescription", "")
          if (cd.contains(expected, ignoreCase = true)) { found = true; break }
        }
      }
      if (found) {
        reasons.put("contentDesc matched: $expected")
      } else {
        reasons.put("contentDesc missing: $expected")
        matched = false
      }
    }
    return matched
  }
}
