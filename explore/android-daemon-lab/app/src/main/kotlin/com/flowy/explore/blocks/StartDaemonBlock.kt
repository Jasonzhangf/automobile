package com.flowy.explore.blocks

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.flowy.explore.runtime.DaemonForegroundService

class StartDaemonBlock(private val context: Context) {
  fun run() {
    val intent = Intent(context, DaemonForegroundService::class.java).apply {
      action = DaemonForegroundService.ACTION_START
    }
    ContextCompat.startForegroundService(context, intent)
  }
}
