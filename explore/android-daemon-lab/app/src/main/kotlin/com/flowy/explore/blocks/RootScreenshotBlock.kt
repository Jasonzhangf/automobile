package com.flowy.explore.blocks

import com.flowy.explore.foundation.RootShellRunner

class RootScreenshotBlock(
  private val runRoot: (String) -> RootShellRunner.Result = { RootShellRunner().run(it) },
) {
  fun run(): ByteArray {
    val command = "tmp=/data/local/tmp/flowy-root-screenshot.png; screencap -p \$tmp >/dev/null 2>&1 && cat \$tmp && rm -f \$tmp"
    val result = runRoot(command)
    check(result.exitCode == 0) { "ROOT_SCREENSHOT_FAILED" }
    check(result.stdoutBytes.isNotEmpty()) { "ROOT_SCREENSHOT_EMPTY" }
    return result.stdoutBytes
  }
}
