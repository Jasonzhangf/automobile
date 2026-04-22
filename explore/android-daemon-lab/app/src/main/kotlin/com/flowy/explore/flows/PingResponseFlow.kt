package com.flowy.explore.flows

import com.flowy.explore.blocks.AppendLogBlock
import com.flowy.explore.blocks.HandlePingBlock
import com.flowy.explore.foundation.WsClientAdapter

class PingResponseFlow(
  private val appendLogBlock: AppendLogBlock,
  private val handlePingBlock: HandlePingBlock,
  private val wsClientAdapter: WsClientAdapter,
) {
  fun run(requestId: String, runId: String, command: String) {
    appendLogBlock.info("command_started", "starting ping", requestId, runId, command)
    wsClientAdapter.send(handlePingBlock.run(requestId, runId, command).toString())
    appendLogBlock.info("command_finished", "finished ping", requestId, runId, command)
  }
}
