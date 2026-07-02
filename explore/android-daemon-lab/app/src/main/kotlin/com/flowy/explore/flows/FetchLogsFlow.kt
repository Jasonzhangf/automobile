package com.flowy.explore.flows

import com.flowy.explore.blocks.AppendLogBlock
import com.flowy.explore.blocks.HandleFetchLogsBlock
import com.flowy.explore.foundation.WsClientAdapter
import org.json.JSONObject

class FetchLogsFlow(
  private val appendLogBlock: AppendLogBlock,
  private val handleFetchLogsBlock: HandleFetchLogsBlock,
  private val wsClientAdapter: WsClientAdapter,
) {
  fun run(requestId: String, runId: String, command: String, tail: Int) {
    appendLogBlock.info("logs_read_started", "reading logs", requestId, runId, command)
    val payload = JSONObject().apply {
      put("tail", tail)
    }
    val result = handleFetchLogsBlock.run(payload)
    wsClientAdapter.send(result)
    appendLogBlock.info("logs_read_finished", "sent logs", requestId, runId, command)
  }
}
