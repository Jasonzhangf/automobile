package com.flowy.explore.blocks

import com.flowy.explore.runtime.LocalLogStore

class AppendLogBlock(private val logStore: LocalLogStore) {
  fun info(event: String, message: String, requestId: String? = null, runId: String? = null, command: String? = null) {
    logStore.append("info", event, message, requestId, runId, command)
  }

  fun error(event: String, message: String, requestId: String? = null, runId: String? = null, command: String? = null) {
    logStore.append("error", event, message, requestId, runId, command)
  }
}
