package com.flowy.explore.blocks

import com.flowy.explore.foundation.RootShellRunner
import com.flowy.explore.support.FakeProcess
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Test

class InputTextBlockTest {
  @Test
  fun run_rootUsesClipboardForInput() {
    val commands = mutableListOf<List<String>>()
    var clipboardText: String? = null
    val block = InputTextBlock(
      RootShellRunner(
        candidates = listOf("su"),
        processFactory = { command ->
          commands += command
          FakeProcess()
        },
      ),
      setClipboard = { text -> clipboardText = text; true },
    )

    val message = block.run(JSONObject().put("backend", "root").put("text", "露营装备"))

    assertEquals("input:root:clipboard:4", message)
    assertEquals("露营装备", clipboardText)
    assertEquals(1, commands.size)
    assert(commands[0].any { it.contains("keyevent") })
  }

  @Test(expected = IllegalStateException::class)
  fun run_rejectsMissingText() {
    InputTextBlock(
      RootShellRunner(candidates = listOf("su"), processFactory = { FakeProcess() }),
    ).run(JSONObject().put("backend", "root"))
  }
}
