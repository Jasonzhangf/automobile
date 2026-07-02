package com.flowy.explore.runtime.adapter

import android.content.Context
import android.content.Intent
import com.flowy.explore.foundation.executor.ServiceController
import com.flowy.explore.runtime.DaemonForegroundService

class DaemonServiceController(private val context: Context) : ServiceController {
  override fun start(): Boolean {
    val intent = DaemonForegroundService.intent(context)
    context.startForegroundService(intent)
    return true
  }

  override fun stop(): Boolean {
    val intent = DaemonForegroundService.stopIntent(context)
    context.startService(intent)
    return true
  }

  override fun buildStartIntent(): Intent? {
    return DaemonForegroundService.intent(context)
  }
}
