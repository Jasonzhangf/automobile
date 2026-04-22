package com.flowy.explore.foundation

import android.content.Context
import android.os.Build
import android.provider.Settings

class OverlayPermissionReader(private val context: Context) {
  fun isGranted(): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      Settings.canDrawOverlays(context)
    } else {
      true
    }
  }

  fun statusText(): String = if (isGranted()) "granted" else "missing"
}
