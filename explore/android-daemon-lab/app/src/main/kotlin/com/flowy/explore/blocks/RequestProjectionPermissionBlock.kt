package com.flowy.explore.blocks

import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager

class RequestProjectionPermissionBlock(private val context: Context) {
  fun createIntent(): Intent {
    val manager = context.getSystemService(MediaProjectionManager::class.java)
    return manager.createScreenCaptureIntent()
  }
}
