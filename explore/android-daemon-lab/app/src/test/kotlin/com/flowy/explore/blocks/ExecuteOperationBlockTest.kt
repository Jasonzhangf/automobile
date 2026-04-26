package com.flowy.explore.blocks

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExecuteOperationBlockTest {
  @Test
  fun run_returnsStandardSuccessShape() {
    val block = ExecuteOperationBlock(
      tap = { "tap:accessibility:1,2" },
      scroll = { "scroll:accessibility:down" },
      inputText = { "input:accessibility:3" },
      back = { "back:accessibility" },
      pressKey = { "press-key:accessibility:home" },
      openDeepLink = { "open-app:com.xingin.xhs" },
      runRootCommand = { "root:id:uid=0(root)" },
      now = { "2026-04-26T10:00:00+08:00" },
    )

    val result = block.run("tap", JSONObject(), "tap_search_entry")

    assertEquals("flowy-block-result-v1", result.getString("schemaVersion"))
    assertEquals("ok", result.getString("status"))
    assertEquals("tap_search_entry", result.getJSONObject("output").getString("operationId"))
    assertEquals("tap:accessibility:1,2", result.getJSONObject("output").getString("message"))
    assertTrue(result.isNull("error"))
  }

  @Test
  fun run_returnsStandardErrorShape() {
    val block = ExecuteOperationBlock(
      tap = { error("TAP_FAILED") },
      scroll = { "scroll:accessibility:down" },
      inputText = { "input:accessibility:3" },
      back = { "back:accessibility" },
      pressKey = { "press-key:accessibility:home" },
      openDeepLink = { "open-app:com.xingin.xhs" },
      runRootCommand = { "root:id:uid=0(root)" },
      now = { "2026-04-26T10:00:00+08:00" },
    )

    val result = block.run("tap")

    assertEquals("error", result.getString("status"))
    assertEquals("TAP_FAILED", result.getJSONObject("error").getString("code"))
    assertEquals(false, result.getJSONObject("error").getBoolean("retryable"))
  }

  @Test
  fun run_supportsOpenDeepLink() {
    val block = ExecuteOperationBlock(
      tap = { "tap:accessibility:1,2" },
      scroll = { "scroll:accessibility:down" },
      inputText = { "input:accessibility:3" },
      back = { "back:accessibility" },
      pressKey = { "press-key:accessibility:home" },
      openDeepLink = { payload -> "open-app:${payload.getString("packageName")}" },
      runRootCommand = { "root:id:uid=0(root)" },
      now = { "2026-04-26T10:00:00+08:00" },
    )

    val result = block.run("open-deep-link", JSONObject().put("packageName", "com.xingin.xhs"))

    assertEquals("ok", result.getString("status"))
    assertEquals("open-app:com.xingin.xhs", result.getJSONObject("output").getString("message"))
  }

  @Test
  fun run_supportsRootCommandProbe() {
    val block = ExecuteOperationBlock(
      tap = { "tap:accessibility:1,2" },
      scroll = { "scroll:accessibility:down" },
      inputText = { "input:accessibility:3" },
      back = { "back:accessibility" },
      pressKey = { "press-key:accessibility:home" },
      openDeepLink = { "open-app:com.xingin.xhs" },
      runRootCommand = { payload -> "root:${payload.getString("probe")}:uid=0(root)" },
      now = { "2026-04-26T10:00:00+08:00" },
    )

    val result = block.run("run-root-command", JSONObject().put("probe", "id"))

    assertEquals("ok", result.getString("status"))
    assertEquals("root:id:uid=0(root)", result.getJSONObject("output").getString("message"))
  }
}
