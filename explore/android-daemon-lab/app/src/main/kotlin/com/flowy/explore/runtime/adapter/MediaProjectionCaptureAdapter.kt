package com.flowy.explore.runtime.adapter

import com.flowy.explore.foundation.executor.ProjectionCapture
import com.flowy.explore.runtime.MediaProjectionSessionHolder

/**
 * Adapter for MediaProjection-based screen capture.
 * Uses MediaProjectionSessionHolder.capture() instead of the non-existent current().
 */
object MediaProjectionCaptureAdapter : ProjectionCapture {
  override fun captureToFile(filePath: String): Boolean {
    return try {
      val capture = MediaProjectionSessionHolder.capture()
      capture.captureToFile(filePath)
    } catch (t: Throwable) {
      false
    }
  }
}
