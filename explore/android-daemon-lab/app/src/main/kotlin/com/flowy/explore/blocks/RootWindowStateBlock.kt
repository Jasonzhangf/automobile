package com.flowy.explore.blocks

import com.flowy.explore.foundation.RootShellRunner

class RootWindowStateBlock(
  private val runRoot: (String) -> RootShellRunner.Result = { RootShellRunner().run(it) },
) {
  fun run(): Snapshot {
    val result = runRoot("dumpsys window | grep -E 'mCurrentFocus|mFocusedApp'")
    check(result.exitCode == 0) { "ROOT_WINDOW_STATE_FAILED" }
    val raw = result.stdoutText().ifBlank { error("ROOT_WINDOW_STATE_EMPTY") }
    return Snapshot(
      rawText = raw,
      packageName = parsePackageName(raw),
      windowTitle = parseWindowTitle(raw),
    )
  }

  fun parsePackageName(raw: String): String? {
    val match = Regex("([a-zA-Z0-9_]+(?:\\.[a-zA-Z0-9_]+)+)/").find(raw)
    return match?.groupValues?.get(1)
  }

  fun parseWindowTitle(raw: String): String? {
    return raw.lineSequence().firstOrNull { it.contains("mCurrentFocus") }?.trim()
  }

  data class Snapshot(
    val rawText: String,
    val packageName: String?,
    val windowTitle: String?,
  )
}
