package com.flowy.explore.blocks

import com.flowy.explore.foundation.RootShellRunner
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Test

class RootCommandBlockTest {
  @Test
  fun run_supportsRootStatusProbe() {
    val block = RootCommandBlock { command ->
      assertEquals("id && whoami && getenforce", command)
      RootShellRunner.Result(0, "uid=0(root)\nroot\nPermissive".toByteArray())
    }

    val result = block.run(JSONObject().put("probe", "root-status"))

    assertEquals("root:root-status:uid=0(root)\nroot\nPermissive", result)
  }

  @Test(expected = IllegalStateException::class)
  fun run_rejectsUnsupportedProbe() {
    RootCommandBlock { RootShellRunner.Result(0, "ok".toByteArray()) }
      .run(JSONObject().put("probe", "rm -rf /"))
  }

  @Test(expected = IllegalStateException::class)
  fun run_failsWhenRootCommandFails() {
    RootCommandBlock { RootShellRunner.Result(1, "denied".toByteArray()) }
      .run(JSONObject().put("probe", "id"))
  }
}
