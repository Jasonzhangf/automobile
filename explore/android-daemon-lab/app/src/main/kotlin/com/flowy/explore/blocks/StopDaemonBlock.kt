package com.flowy.explore.blocks

import android.content.Context
import android.content.Intent
import com.flowy.explore.runtime.DaemonForegroundService

class StopDaemonBlock(private val context: Context) {
  fun run() {
    val intent = Intent(context, DaemonForegroundService::class.java).apply {
      action = DaemonForegroundService.ACTION_STOP
    }
    context.startService(intent)
  }
}
