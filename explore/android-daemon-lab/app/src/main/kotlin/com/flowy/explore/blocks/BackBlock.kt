package com.flowy.explore.blocks

import com.flowy.explore.foundation.OperationBackend
import com.flowy.explore.foundation.RootShellRunner
import com.flowy.explore.runtime.FlowyAccessibilityService
import org.json.JSONObject

class BackBlock(
  private val rootShellRunner: RootShellRunner = RootShellRunner(),
) {
  fun run(payload: JSONObject): String {
    return when (OperationBackend.fromPayload(payload)) {
      OperationBackend.ACCESSIBILITY -> {
        check(FlowyAccessibilityService.requireInstance().performBack()) { "BACK_FAILED" }
        "back:accessibility"
      }
      OperationBackend.ROOT -> {
        val result = rootShellRunner.run("input keyevent 4")
        check(result.exitCode == 0) { "BACK_FAILED" }
        "back:root"
      }
    }
  }
}
