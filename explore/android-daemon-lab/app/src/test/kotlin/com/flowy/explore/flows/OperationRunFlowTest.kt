package com.flowy.explore.flows

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OperationRunFlowTest {
  @Test
  fun run_sendsSuccessResponse() {
    val sent = mutableListOf<String>()
    val logs = mutableListOf<String>()
    val flow = OperationRunFlow(
      logInfo = { event, _, _, _, _ -> logs += event },
      logError = { event, _, _, _, _ -> logs += event },
      versionName = { "0.1.0078" },
      sendResponse = {
        sent += it
        true
      },
      executeOperation = { _, _, _ ->
        JSONObject("""{"status":"ok","output":{"message":"tap:root:1,2"}}""")
      },
      deviceInfo = { Triple("device-1", "model-1", "16") },
    )

    flow.run("req1", "run1", "tap", JSONObject())

    assertTrue(sent.single().contains("\"status\":\"ok\""))
    assertTrue(sent.single().contains("\"message\":\"tap:root:1,2\""))
    assertEquals(listOf("operation_started", "operation_finished", "command_finished"), logs)
  }

  @Test
  fun run_sendsErrorResponse() {
    val sent = mutableListOf<String>()
    val logs = mutableListOf<String>()
    val flow = OperationRunFlow(
      logInfo = { event, _, _, _, _ -> logs += event },
      logError = { event, _, _, _, _ -> logs += event },
      versionName = { "0.1.0078" },
      sendResponse = {
        sent += it
        true
      },
      executeOperation = { _, _, _ ->
        JSONObject("""{"status":"error","error":{"code":"TAP_FAILED"}}""")
      },
      deviceInfo = { Triple("device-1", "model-1", "16") },
    )

    flow.run("req1", "run1", "tap", JSONObject())

    assertTrue(sent.single().contains("\"status\":\"error\""))
    assertTrue(sent.single().contains("\"code\":\"TAP_FAILED\""))
    assertEquals(listOf("operation_started", "operation_failed", "command_failed"), logs)
  }
}
