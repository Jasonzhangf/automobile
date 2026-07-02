package com.flowy.explore.runtime.adapter

import com.flowy.explore.foundation.executor.LogStore
import com.flowy.explore.runtime.LocalLogStore

object LocalLogStoreAdapter : LogStore {
  override fun append(message: String, level: String, event: String): Boolean {
    return LocalLogStore.append(message, level, event)
  }

  override fun tail(maxBytes: Int): String {
    return LocalLogStore.tail(maxBytes)
  }
}
