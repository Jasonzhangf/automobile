package com.flowy.explore.foundation.executor

/**
 * Abstraction over local log storage.
 * Allows blocks to depend on foundation interface instead of LocalLogStore runtime.
 */
interface LogStore {
  fun append(message: String, level: String = "info", event: String = ""): Boolean
  fun tail(maxBytes: Int): String
}
