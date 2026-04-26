package com.flowy.explore.foundation

import org.json.JSONObject

enum class OperationBackend {
  ACCESSIBILITY,
  ROOT,
  ;

  companion object {
    fun fromPayload(payload: JSONObject, defaultBackend: OperationBackend = ACCESSIBILITY): OperationBackend {
      return when (payload.optString("backend").ifBlank { defaultBackend.name }.lowercase()) {
        "accessibility" -> ACCESSIBILITY
        "root" -> ROOT
        else -> error("UNSUPPORTED_OPERATION_BACKEND")
      }
    }
  }
}
