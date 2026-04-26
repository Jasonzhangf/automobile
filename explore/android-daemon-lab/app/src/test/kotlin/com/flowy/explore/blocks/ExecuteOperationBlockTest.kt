package com.flowy.explore.blocks

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExecuteOperationBlockTest {
  @Test
  fun run_returnsStandardSuccessShape() {
    val block = ExecuteOperationBlock(
      tap = { "tap:1,2" },
      scroll = { "scroll:down" },
      inputText = { "input:3" },
      back = { "back" },
      pressKey = { "press-key:home" },
      now = { "2026-04-26T10:00:00+08:00" },
    )

    val result = block.run("tap", JSONObject(), "tap_search_entry")

    assertEquals("flowy-block-result-v1", result.getString("schemaVersion"))
    assertEquals("ok", result.getString("status"))
    assertEquals("tap_search_entry", result.getJSONObject("output").getString("operationId"))
    assertEquals("tap:1,2", result.getJSONObject("output").getString("message"))
    assertTrue(result.isNull("error"))
  }

  @Test
  fun run_returnsStandardErrorShape() {
    val block = ExecuteOperationBlock(
      tap = { error("TAP_FAILED") },
      scroll = { "scroll:down" },
      inputText = { "input:3" },
      back = { "back" },
      pressKey = { "press-key:home" },
      now = { "2026-04-26T10:00:00+08:00" },
    )

    val result = block.run("tap")

    assertEquals("error", result.getString("status"))
    assertEquals("TAP_FAILED", result.getJSONObject("error").getString("code"))
    assertEquals(false, result.getJSONObject("error").getBoolean("retryable"))
  }
}
