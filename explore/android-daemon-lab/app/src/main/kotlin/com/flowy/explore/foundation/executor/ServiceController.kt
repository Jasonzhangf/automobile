package com.flowy.explore.foundation.executor

import android.content.Intent

/**
 * Abstraction over Android service lifecycle.
 * Allows blocks to depend on the interface instead of DaemonForegroundService.
 */
interface ServiceController {
  fun start(): Boolean
  fun stop(): Boolean
  fun buildStartIntent(): Intent?
}
