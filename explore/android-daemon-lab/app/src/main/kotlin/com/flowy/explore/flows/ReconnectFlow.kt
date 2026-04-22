package com.flowy.explore.flows

import com.flowy.explore.blocks.ReconnectWsBlock
import java.util.concurrent.ScheduledExecutorService

class ReconnectFlow(scheduler: ScheduledExecutorService) {
  private val reconnectWsBlock = ReconnectWsBlock(scheduler)
  private var delays = listOf(1L, 2L, 5L, 10L)
  private var index = 0

  fun schedule(action: () -> Unit) {
    val delay = delays[index.coerceAtMost(delays.lastIndex)]
    if (index < delays.lastIndex) index += 1
    reconnectWsBlock.run(delay, action)
  }

  fun reset() {
    index = 0
  }
}
