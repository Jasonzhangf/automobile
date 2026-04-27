package com.flowy.explore.blocks

import com.flowy.explore.foundation.RootShellRunner

class SelfExemptionBlock(
  private val rootRunner: RootShellRunner = RootShellRunner(),
) {
  data class Result(val success: Boolean, val detail: String)

  fun run(packageName: String, uid: Int = 0): Result {
    val cmds = listOf(
      "cmd deviceidle whitelist +$packageName",
      "cmd appops set $packageName RUN_IN_BACKGROUND allow",
      "cmd appops set $packageName RUN_ANY_IN_BACKGROUND allow",
      "cmd appops set $packageName RUN_IN_BACKGROUND allow --uid $packageName",
      "settings put global hidden_api_policy 1",
    )
    val results = cmds.map { cmd ->
      try {
        val r = rootRunner.run(cmd)
        "ok:$cmd"
      } catch (t: Throwable) {
        "fail:$cmd:${t.message}"
      }
    }
    val allOk = results.all { it.startsWith("ok") }
    return Result(allOk, results.joinToString("; "))
  }
}
