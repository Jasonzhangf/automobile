package com.flowy.explore.blocks

import com.flowy.explore.foundation.RootShellRunner
import org.json.JSONObject

class RootCommandBlock(
  private val runRoot: (String) -> RootShellRunner.Result = { RootShellRunner().run(it) },
) {
  fun run(payload: JSONObject): String {
    val probe = payload.optString("probe").ifBlank { error("ROOT_PROBE_MISSING") }
    val command = commandFor(probe)
    val result = runRoot(command)
    check(result.exitCode == 0) { "ROOT_COMMAND_FAILED" }
    val stdout = result.stdoutText().ifBlank { "(empty)" }
    return "root:$probe:$stdout"
  }

  fun commandFor(probe: String): String {
    return when (probe.lowercase()) {
      "id" -> "id"
      "whoami" -> "whoami"
      "getenforce" -> "getenforce"
      "root-status" -> "id && whoami && getenforce"
      else -> error("UNSUPPORTED_ROOT_PROBE")
    }
  }
}
