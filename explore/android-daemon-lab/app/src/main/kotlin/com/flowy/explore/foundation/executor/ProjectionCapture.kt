package com.flowy.explore.foundation.executor

/**
 * Abstraction over screenshot capture. Allows blocks to depend on
 * foundation interface instead of MediaProjection runtime.
 */
interface ProjectionCapture {
  fun captureToFile(filePath: String): Boolean
}
