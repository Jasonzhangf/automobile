package com.flowy.explore.blocks

import android.content.Context
import android.content.Intent
import android.provider.Settings

class OpenAccessibilitySettingsBlock(private val context: Context) {
  fun run() {
    context.startActivity(
      Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      },
    )
  }
}
