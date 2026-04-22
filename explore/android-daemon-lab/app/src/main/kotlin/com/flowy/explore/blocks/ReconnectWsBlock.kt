package com.flowy.explore.blocks

import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

class ReconnectWsBlock(private val scheduler: ScheduledExecutorService) {
  fun run(delaySeconds: Long, action: () -> Unit) {
    scheduler.schedule(action, delaySeconds, TimeUnit.SECONDS)
  }
}
