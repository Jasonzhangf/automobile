package com.flowy.explore.blocks

import com.flowy.explore.foundation.executor.LogStore
import com.flowy.explore.runtime.adapter.LocalLogStoreAdapter
import org.json.JSONObject

class HandleFetchLogsBlock(
  private val logStore: LogStore = LocalLogStoreAdapter,
) {
  fun run(payload: JSONObject): String {
    val maxBytes = payload.optInt("maxBytes", 8192)
    return logStore.tail(maxBytes)
  }
}
