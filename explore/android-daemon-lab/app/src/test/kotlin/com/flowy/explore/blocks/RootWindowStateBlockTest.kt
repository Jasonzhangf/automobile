package com.flowy.explore.blocks

import com.flowy.explore.foundation.RootShellRunner
import org.junit.Assert.assertEquals
import org.junit.Test

class RootWindowStateBlockTest {
  @Test
  fun run_parsesCurrentFocusPackage() {
    val raw = "mCurrentFocus=Window{123 u0 com.xingin.xhs/com.xingin.xhs.index.v2.IndexActivityV2}\n"
    val block = RootWindowStateBlock { RootShellRunner.Result(0, raw.toByteArray()) }

    val snapshot = block.run()

    assertEquals("com.xingin.xhs", snapshot.packageName)
    assertEquals(raw.trim(), snapshot.windowTitle)
  }

  @Test(expected = IllegalStateException::class)
  fun run_rejectsEmptyOutput() {
    RootWindowStateBlock { RootShellRunner.Result(0, byteArrayOf()) }.run()
  }
}
