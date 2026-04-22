package com.flowy.explore.blocks

import android.content.Context
import android.content.Intent
import com.flowy.explore.ui.workbench.WorkbenchOverlayService

class StopOverlayWorkbenchBlock(private val context: Context) {
  fun run() {
    context.startService(Intent(context, WorkbenchOverlayService::class.java).apply {
      action = WorkbenchOverlayService.ACTION_HIDE
    })
  }
}
