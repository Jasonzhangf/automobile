package com.flowy.explore.blocks

import com.flowy.explore.foundation.BlockResultFactory
import com.flowy.explore.foundation.TimeHelper
import org.json.JSONObject

class EmitEventBlock(
  private val sender: (String) -> Boolean,
  private val now: () -> String = TimeHelper::now,
) {
  fun run(eventType: String, requestId: String, runId: String, data: JSONObject = JSONObject()): JSONObject {
    val startedAt = now()
    return try {
      if (eventType.isBlank()) error("EVENT_TYPE_MISSING")
      if (requestId.isBlank()) error("REQUEST_ID_MISSING")
      if (runId.isBlank()) error("RUN_ID_MISSING")
      val accepted = sender(
        JSONObject().apply {
          put("type", eventType)
          put("requestId", requestId)
          put("runId", runId)
          put("sentAt", now())
          put("data", data)
        }.toString(),
      )
      BlockResultFactory.ok(
        startedAt = startedAt,
        output = JSONObject().apply {
          put("accepted", accepted)
        },
      )
    } catch (throwable: Throwable) {
      val code = throwable.message ?: "EMIT_EVENT_FAILED"
      BlockResultFactory.error(startedAt = startedAt, code = code, message = code)
    }
  }
}
