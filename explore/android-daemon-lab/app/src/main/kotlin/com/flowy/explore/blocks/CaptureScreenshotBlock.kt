package com.flowy.explore.blocks

import com.flowy.explore.runtime.MediaProjectionSessionHolder
import com.flowy.explore.runtime.ProjectionCapture

class CaptureScreenshotBlock {
  fun run(): ProjectionCapture {
    return MediaProjectionSessionHolder.capture()
  }
}
