package com.flowy.explore.blocks

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings

class OpenOverlayPermissionSettingsBlock(private val context: Context) {
  fun run() {
    context.startActivity(
      Intent(
        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
        Uri.parse("package:${context.packageName}"),
      ).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      },
    )
  }
}
