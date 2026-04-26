package com.flowy.explore.blocks

import com.flowy.explore.foundation.RootShellRunner
import com.flowy.explore.support.FakeProcess
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Test

class BackBlockTest {
  @Test
  fun run_rootSendsBackKeyevent() {
    val commands = mutableListOf<List<String>>()
    val block = BackBlock(
      RootShellRunner(
        candidates = listOf("su"),
        processFactory = { command ->
          commands += command
          FakeProcess()
        },
      ),
    )

    val message = block.run(JSONObject().put("backend", "root"))

    assertEquals("back:root", message)
    assertEquals(listOf("su", "-c", "input keyevent 4"), commands.single())
  }
}
