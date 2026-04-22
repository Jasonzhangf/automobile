package com.flowy.explore.foundation

import android.content.Context
import android.hardware.display.DisplayManager
import android.util.DisplayMetrics
import android.view.Display
import android.view.Surface

data class DisplayInfo(
  val widthPx: Int,
  val heightPx: Int,
  val densityDpi: Int,
  val rotation: Int,
  val displayId: Int,
)

class DisplayInfoReader(private val context: Context) {
  fun read(): DisplayInfo {
    val display = context.getSystemService(DisplayManager::class.java).getDisplay(Display.DEFAULT_DISPLAY)
    val metrics = DisplayMetrics()
    if (display != null) {
      display.getRealMetrics(metrics)
    } else {
      metrics.setTo(context.resources.displayMetrics)
    }
    return DisplayInfo(
      widthPx = metrics.widthPixels,
      heightPx = metrics.heightPixels,
      densityDpi = metrics.densityDpi,
      rotation = display?.rotation ?: Surface.ROTATION_0,
      displayId = display?.displayId ?: 0,
    )
  }
}
