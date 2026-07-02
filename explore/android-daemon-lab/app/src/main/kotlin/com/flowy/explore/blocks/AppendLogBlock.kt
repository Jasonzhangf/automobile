package com.flowy.explore.blocks

import com.flowy.explore.foundation.executor.LogStore
import com.flowy.explore.runtime.adapter.LocalLogStoreAdapter
import org.json.JSONObject

/**
 * AppendLogBlock logs messages through the LogStore interface.
 *
 * Two usage modes:
 * 1. run(payload) — WS dispatch path: parse JSON payload
 * 2. info/error/success wrappers — legacy flow path (for backward compat)
 */
class AppendLogBlock(
  private val logStore: LogStore = LocalLogStoreAdapter,
) {
  fun run(payload: JSONObject): String {
    val message = payload.optString("message", "")
    val level = payload.optString("level", "info")
    val event = payload.optString("event", "")
    check(message.isNotBlank()) { "EMPTY_LOG_MESSAGE" }
    check(logStore.append(message, level, event)) { "LOG_APPEND_FAILED" }
    return "log-append:$level"
  }

  // Legacy compat: info(event, message, requestId, runId, command)
  fun info(event: String, message: String, requestId: String? = null, runId: String? = null, command: String? = null) {
    logStore.append(message, "info", event)
  }

  // Legacy compat: error(event, message, requestId, runId, command)
  fun error(event: String, message: String, requestId: String? = null, runId: String? = null, command: String? = null) {
    logStore.append(message, "error", event)
  }

  // Legacy compat: success(event, message, requestId, runId, command)
  fun success(event: String, message: String, requestId: String? = null, runId: String? = null, command: String? = null) {
    logStore.append(message, "success", event)
  }
}
