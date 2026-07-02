package com.flowy.explore.foundation.executor

/**
 * Abstraction over tap execution. Allows blocks to depend on the
 * foundation interface instead of the runtime service.
 */
interface TapExecutor {
  fun tap(x: Int, y: Int): Boolean
}
