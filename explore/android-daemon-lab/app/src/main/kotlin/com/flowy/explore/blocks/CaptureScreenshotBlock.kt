package com.flowy.explore.blocks

import android.content.Context
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Handler
import android.os.Looper
import com.flowy.explore.foundation.BitmapEncoder
import com.flowy.explore.foundation.DisplayInfo
import com.flowy.explore.foundation.DisplayInfoReader
import com.flowy.explore.runtime.DaemonForegroundService
import com.flowy.explore.runtime.MediaProjectionSessionHolder
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

data class ScreenshotCapture(
  val pngBytes: ByteArray,
  val displayInfo: DisplayInfo,
)

class CaptureScreenshotBlock(private val context: Context) {
  fun run(): ScreenshotCapture {
    val resultCode = MediaProjectionSessionHolder.resultCode()
      ?: error("SCREENSHOT_PERMISSION_NOT_READY")
    val dataIntent = MediaProjectionSessionHolder.dataIntent()
      ?: error("SCREENSHOT_PERMISSION_NOT_READY")
    DaemonForegroundService.promoteProjectionIfReady()
    val projectionManager = context.getSystemService(MediaProjectionManager::class.java)
    val mediaProjection = projectionManager.getMediaProjection(resultCode, dataIntent)
      ?: error("SCREENSHOT_CAPTURE_FAILED")
    val displayInfo = DisplayInfoReader(context).read()
    val imageReader = ImageReader.newInstance(
      displayInfo.widthPx,
      displayInfo.heightPx,
      PixelFormat.RGBA_8888,
      2,
    )
    val latch = CountDownLatch(1)
    val handler = Handler(Looper.getMainLooper())
    val callback = object : MediaProjection.Callback() {}
    mediaProjection.registerCallback(callback, handler)
    imageReader.setOnImageAvailableListener({ latch.countDown() }, handler)
    val virtualDisplay = mediaProjection.createVirtualDisplay(
      "flowy-screenshot",
      displayInfo.widthPx,
      displayInfo.heightPx,
      displayInfo.densityDpi,
      DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
      imageReader.surface,
      null,
      null,
    )
    try {
      if (!latch.await(3, TimeUnit.SECONDS)) error("SCREENSHOT_CAPTURE_FAILED")
      val image = imageReader.acquireLatestImage() ?: error("SCREENSHOT_CAPTURE_FAILED")
      return image.use {
        val bitmap = it.toBitmap(displayInfo.widthPx, displayInfo.heightPx)
        val pngBytes = BitmapEncoder.toPng(bitmap)
        bitmap.recycle()
        ScreenshotCapture(pngBytes = pngBytes, displayInfo = displayInfo)
      }
    } finally {
      virtualDisplay.release()
      imageReader.close()
      mediaProjection.unregisterCallback(callback)
      mediaProjection.stop()
    }
  }

  private fun android.media.Image.use(block: (android.media.Image) -> ScreenshotCapture): ScreenshotCapture {
    return try {
      block(this)
    } finally {
      close()
    }
  }

  private fun android.media.Image.toBitmap(widthPx: Int, heightPx: Int): Bitmap {
    val plane = planes.first()
    val buffer = plane.buffer
    val pixelStride = plane.pixelStride
    val rowStride = plane.rowStride
    val rowPadding = rowStride - pixelStride * widthPx
    val fullBitmap = Bitmap.createBitmap(
      widthPx + rowPadding / pixelStride,
      heightPx,
      Bitmap.Config.ARGB_8888,
    )
    fullBitmap.copyPixelsFromBuffer(buffer)
    return Bitmap.createBitmap(fullBitmap, 0, 0, widthPx, heightPx).also {
      fullBitmap.recycle()
    }
  }
}
