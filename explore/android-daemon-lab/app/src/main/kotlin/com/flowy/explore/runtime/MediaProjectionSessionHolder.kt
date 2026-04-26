package com.flowy.explore.runtime

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.flowy.explore.foundation.BitmapEncoder
import com.flowy.explore.foundation.DisplayInfo
import com.flowy.explore.foundation.DisplayInfoReader
import com.flowy.explore.foundation.TimeHelper
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

object MediaProjectionSessionHolder {
  private const val TAG = "FlowyProjection"
  @Volatile private var grantedAt: String? = null
  @Volatile private var pendingGrant: PendingGrant? = null
  @Volatile private var activeSession: ActiveSession? = null
  @Volatile private var lastError: String? = null

  fun store(context: Context, resultCode: Int, data: Intent?) {
    clear()
    if (resultCode != Activity.RESULT_OK || data == null) {
      lastError = "permission-denied"
      Log.w(TAG, "projection permission denied")
      return
    }
    grantedAt = TimeHelper.now()
    pendingGrant = PendingGrant(resultCode, Intent(data))
    activate(context.applicationContext)
  }

  fun clear() {
    activeSession?.close()
    activeSession = null
    pendingGrant = null
    grantedAt = null
    lastError = null
  }

  fun hasGrant(): Boolean = pendingGrant != null || activeSession != null

  fun isReady(): Boolean = activeSession?.isReady() == true

  fun statusText(): String {
    val grantedAtValue = grantedAt
    return if (isReady()) {
      "ready${grantedAtValue?.let { " @ $it" } ?: ""}"
    } else if (pendingGrant != null) {
      "grant-pending${lastError?.let { " ($it)" } ?: ""}"
    } else if (lastError != null) {
      "error ($lastError)"
    } else {
      "not-ready"
    }
  }

  @Synchronized
  fun activate(context: Context): Boolean {
    if (activeSession?.isReady() == true) return true
    val grant = pendingGrant ?: return false
    return try {
      activeSession = ActiveSession.create(context.applicationContext, grant.resultCode, Intent(grant.data))
      pendingGrant = null
      lastError = null
      Log.i(TAG, "projection session activated")
      true
    } catch (throwable: Throwable) {
      lastError = throwable.message ?: throwable.javaClass.simpleName
      Log.e(TAG, "projection session activation failed", throwable)
      false
    }
  }

  fun capture(): ProjectionCapture {
    return activeSession?.capture() ?: error("SCREENSHOT_PERMISSION_NOT_READY")
  }

  private data class PendingGrant(
    val resultCode: Int,
    val data: Intent,
  )

  private class ActiveSession private constructor(
    private val mediaProjection: MediaProjection,
    private val virtualDisplay: VirtualDisplay,
    private val imageReader: ImageReader,
    private val displayInfo: DisplayInfo,
    private val handler: Handler,
    private val stopCallback: MediaProjection.Callback,
  ) {
    @Volatile private var closed = false

    fun isReady(): Boolean = !closed

    @Synchronized
    fun capture(): ProjectionCapture {
      check(!closed) { "SCREENSHOT_PERMISSION_NOT_READY" }
      val immediate = imageReader.acquireLatestImage()
      if (immediate != null) {
        return immediate.use { image ->
          ProjectionCapture(toPng(image), displayInfo)
        }
      }
      val latch = CountDownLatch(1)
      imageReader.setOnImageAvailableListener({ latch.countDown() }, handler)
      try {
        if (!latch.await(2, TimeUnit.SECONDS)) error("SCREENSHOT_CAPTURE_FAILED")
      } finally {
        imageReader.setOnImageAvailableListener(null, null)
      }
      val image = imageReader.acquireLatestImage() ?: error("SCREENSHOT_CAPTURE_FAILED")
      return image.use {
        ProjectionCapture(toPng(it), displayInfo)
      }
    }

    @Synchronized
    fun close() {
      if (closed) return
      closed = true
      imageReader.setOnImageAvailableListener(null, null)
      mediaProjection.unregisterCallback(stopCallback)
      virtualDisplay.release()
      imageReader.close()
      mediaProjection.stop()
      if (activeSession === this) {
        activeSession = null
        grantedAt = null
      }
    }

    private fun toPng(image: android.media.Image): ByteArray {
      val bitmap = image.toBitmap(displayInfo.widthPx, displayInfo.heightPx)
      return try {
        BitmapEncoder.toPng(bitmap)
      } finally {
        bitmap.recycle()
      }
    }

    private fun android.media.Image.use(block: (android.media.Image) -> ProjectionCapture): ProjectionCapture {
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

    companion object {
      fun create(context: Context, resultCode: Int, dataIntent: Intent): ActiveSession {
        val displayInfo = DisplayInfoReader(context).read()
        val handler = Handler(Looper.getMainLooper())
        val projectionManager = context.getSystemService(MediaProjectionManager::class.java)
        val mediaProjection = projectionManager.getMediaProjection(resultCode, dataIntent)
          ?: error("SCREENSHOT_CAPTURE_FAILED")
        var sessionRef: ActiveSession? = null
        val stopCallback = object : MediaProjection.Callback() {
          override fun onStop() {
            sessionRef?.close()
          }
        }
        mediaProjection.registerCallback(stopCallback, handler)
        val imageReader = ImageReader.newInstance(
          displayInfo.widthPx,
          displayInfo.heightPx,
          PixelFormat.RGBA_8888,
          2,
        )
        val virtualDisplay = mediaProjection.createVirtualDisplay(
          "flowy-screenshot",
          displayInfo.widthPx,
          displayInfo.heightPx,
          displayInfo.densityDpi,
          DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
          imageReader.surface,
          null,
          handler,
        )
        val session = ActiveSession(mediaProjection, virtualDisplay, imageReader, displayInfo, handler, stopCallback)
        sessionRef = session
        return session
      }
    }
  }
}

data class ProjectionCapture(
  val pngBytes: ByteArray,
  val displayInfo: DisplayInfo,
)
