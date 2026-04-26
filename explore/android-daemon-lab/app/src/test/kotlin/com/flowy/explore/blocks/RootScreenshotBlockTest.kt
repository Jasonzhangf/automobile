package com.flowy.explore.blocks

import com.flowy.explore.foundation.RootShellRunner
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RootScreenshotBlockTest {
  @Test
  fun run_returnsPngBytes() {
    val png = byteArrayOf(0x89.toByte(), 0x50, 0x4e, 0x47)
    val block = RootScreenshotBlock { command ->
      assertTrue(command.contains("screencap -p"))
      RootShellRunner.Result(0, png)
    }

    assertArrayEquals(png, block.run())
  }

  @Test(expected = IllegalStateException::class)
  fun run_rejectsEmptyBytes() {
    RootScreenshotBlock { RootShellRunner.Result(0, byteArrayOf()) }.run()
  }
}
