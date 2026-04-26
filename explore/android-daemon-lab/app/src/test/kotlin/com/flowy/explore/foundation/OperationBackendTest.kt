package com.flowy.explore.foundation

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Test

class OperationBackendTest {
  @Test
  fun fromPayload_defaultsToAccessibility() {
    assertEquals(OperationBackend.ACCESSIBILITY, OperationBackend.fromPayload(JSONObject()))
  }

  @Test
  fun fromPayload_supportsRoot() {
    assertEquals(OperationBackend.ROOT, OperationBackend.fromPayload(JSONObject().put("backend", "root")))
  }

  @Test(expected = IllegalStateException::class)
  fun fromPayload_rejectsUnknownBackend() {
    OperationBackend.fromPayload(JSONObject().put("backend", "vision"))
  }
}
