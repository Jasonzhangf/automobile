package com.flowy.explore.foundation

import org.json.JSONArray
import org.json.JSONObject

object BlockResultFactory {
  private const val schemaVersion = "flowy-block-result-v1"

  fun ok(
    startedAt: String,
    output: JSONObject,
    artifacts: JSONArray = JSONArray(),
    finishedAt: String = TimeHelper.now(),
    durationMs: Int = 0,
  ): JSONObject {
    return JSONObject().apply {
      put("schemaVersion", schemaVersion)
      put("status", "ok")
      put("startedAt", startedAt)
      put("finishedAt", finishedAt)
      put("durationMs", durationMs)
      put("output", output)
      put("artifacts", artifacts)
      put("error", JSONObject.NULL)
    }
  }

  fun error(
    startedAt: String,
    code: String,
    message: String,
    retryable: Boolean = false,
    artifacts: JSONArray = JSONArray(),
    finishedAt: String = TimeHelper.now(),
    durationMs: Int = 0,
  ): JSONObject {
    return JSONObject().apply {
      put("schemaVersion", schemaVersion)
      put("status", "error")
      put("startedAt", startedAt)
      put("finishedAt", finishedAt)
      put("durationMs", durationMs)
      put("output", JSONObject.NULL)
      put("artifacts", artifacts)
      put("error", JSONObject().apply {
        put("code", code)
        put("message", message)
        put("retryable", retryable)
      })
    }
  }
}
