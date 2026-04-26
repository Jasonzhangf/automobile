package com.flowy.explore.blocks

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EmitEventBlockTest {
  @Test
  fun run_sendsStructuredEvent() {
    var sent = ""
    val block = EmitEventBlock(
      sender = {
        sent = it
        true
      },
      now = { "2026-04-26T10:00:00+08:00" },
    )

    val data = JSONObject()
    data.put("pageSignature", "sig123")
    val result = block.run("workflow.step.succeeded", "req1", "run1", data)

    assertEquals("ok", result.getString("status"))
    assertEquals(true, result.getJSONObject("output").getBoolean("accepted"))
    assertTrue(sent.contains("\"type\":\"workflow.step.succeeded\""))
  }
}
