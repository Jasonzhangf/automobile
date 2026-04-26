package com.flowy.explore.blocks

import com.flowy.explore.foundation.RootShellRunner
import com.flowy.explore.support.FakeProcess
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Test

class InputTextBlockTest {
  @Test
  fun run_rootEscapesTextForShellInput() {
    val commands = mutableListOf<List<String>>()
    val block = InputTextBlock(
      RootShellRunner(
        candidates = listOf("su"),
        processFactory = { command ->
          commands += command
          FakeProcess()
        },
      ),
    )

    val message = block.run(JSONObject().put("backend", "root").put("text", "a b\"c"))

    assertEquals("input:root:5", message)
    assertEquals(listOf("su", "-c", "input text \"a%sb\\\"c\""), commands.single())
  }

  @Test(expected = IllegalStateException::class)
  fun run_rejectsMissingText() {
    InputTextBlock(
      RootShellRunner(candidates = listOf("su"), processFactory = { FakeProcess() }),
    ).run(JSONObject().put("backend", "root"))
  }
}
