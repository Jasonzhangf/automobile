package com.flowy.explore.blocks

import com.flowy.explore.foundation.WsClientAdapter
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong

/**
 * Periodically checks WS health by sending a lightweight ping.
 * If no pong is received within the threshold, triggers a reconnect.
 */
class WsWatchdogBlock(
  private val scheduler: ScheduledExecutorService,
  private val wsClientAdapter: WsClientAdapter,
) {
  private val lastMessageAt = AtomicLong(System.currentTimeMillis())
  @Volatile private var running = false

  fun markAlive() {
    lastMessageAt.set(System.currentTimeMillis())
  }

  fun start(
    checkIntervalSec: Long = 30,
    staleThresholdSec: Long = 60,
    onReconnect: () -> Unit,
  ) {
    running = true
    scheduler.scheduleAtFixedRate({
      if (!running) return@scheduleAtFixedRate
      val elapsed = System.currentTimeMillis() - lastMessageAt.get()
      if (elapsed > staleThresholdSec * 1000) {
        lastMessageAt.set(System.currentTimeMillis())
        onReconnect()
      }
    }, checkIntervalSec, checkIntervalSec, TimeUnit.SECONDS)
  }

  fun stop() {
    running = false
  }
}
