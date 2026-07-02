package com.flowy.explore.blocks

import com.flowy.explore.foundation.executor.LogStore
import com.flowy.explore.runtime.adapter.LocalLogStoreAdapter
import org.json.JSONObject

class ReadLogTailBlock(
  private val logStore: LogStore = LocalLogStoreAdapter,
) {
  fun run(payload: JSONObject): String {
    val maxBytes = payload.optInt("maxBytes", 4096)
    return logStore.tail(maxBytes)
  }
}
