package com.flowy.explore.runtime

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.accessibility.AccessibilityEvent
import com.flowy.explore.foundation.AccessibilityTreeSerializer
import java.lang.ref.WeakReference
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class FlowyAccessibilityService : AccessibilityService() {
  private val serializer = AccessibilityTreeSerializer()
  private val handler = Handler(Looper.getMainLooper())
  private val snapshotRunnable = Runnable { captureSnapshot() }
  private var lastCaptureUptimeMs: Long = 0L

  override fun onServiceConnected() {
    super.onServiceConnected()
    currentInstance = WeakReference(this)
    scheduleSnapshot(initialDelayMs = 200L)
    notifyAvailabilityChanged()
  }

  override fun onAccessibilityEvent(event: AccessibilityEvent?) {
    when (event?.eventType) {
      AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED,
      AccessibilityEvent.TYPE_WINDOWS_CHANGED,
      -> scheduleSnapshot()
    }
  }

  override fun onInterrupt() = Unit

  override fun onDestroy() {
    handler.removeCallbacks(snapshotRunnable)
    AccessibilitySnapshotStore.clear()
    currentInstance?.clear()
    currentInstance = null
    notifyAvailabilityChanged()
    super.onDestroy()
  }

  fun performTap(x: Int, y: Int): Boolean {
    val path = Path().apply {
      moveTo(x.toFloat(), y.toFloat())
      lineTo(x.toFloat(), y.toFloat())
    }
    return dispatchPath(path, 60L)
  }

  fun performSwipe(startX: Int, startY: Int, endX: Int, endY: Int): Boolean {
    val path = Path().apply {
      moveTo(startX.toFloat(), startY.toFloat())
      lineTo(endX.toFloat(), endY.toFloat())
    }
    return dispatchPath(path, 260L)
  }

  fun performBack(): Boolean = performGlobalAction(GLOBAL_ACTION_BACK)

  fun performGlobal(action: Int): Boolean = performGlobalAction(action)

  private fun scheduleSnapshot(initialDelayMs: Long = CAPTURE_DEBOUNCE_MS) {
    handler.removeCallbacks(snapshotRunnable)
    handler.postDelayed(snapshotRunnable, initialDelayMs)
  }

  private fun captureSnapshot() {
    val now = SystemClock.uptimeMillis()
    if (now - lastCaptureUptimeMs < MIN_CAPTURE_GAP_MS) {
      scheduleSnapshot(MIN_CAPTURE_GAP_MS - (now - lastCaptureUptimeMs))
      return
    }
    val root = rootInActiveWindow ?: return
    try {
      AccessibilitySnapshotStore.update(serializer.serialize(root))
      lastCaptureUptimeMs = now
    } finally {
      root.recycle()
    }
  }

  companion object {
    private const val CAPTURE_DEBOUNCE_MS = 350L
    private const val MIN_CAPTURE_GAP_MS = 1000L
    @Volatile private var currentInstance: WeakReference<FlowyAccessibilityService>? = null
    @Volatile private var availabilityListener: (() -> Unit)? = null

    fun requireInstance(): FlowyAccessibilityService {
      return currentInstance?.get() ?: error("ACCESSIBILITY_SERVICE_UNAVAILABLE")
    }

    fun setAvailabilityListener(listener: (() -> Unit)?) {
      availabilityListener = listener
    }

    private fun notifyAvailabilityChanged() {
      availabilityListener?.invoke()
    }
  }

  private fun dispatchPath(path: Path, durationMs: Long): Boolean {
    val latch = CountDownLatch(1)
    var completed = false
    val gesture = GestureDescription.Builder()
      .addStroke(GestureDescription.StrokeDescription(path, 0, durationMs))
      .build()
    val dispatched = dispatchGesture(
      gesture,
      object : GestureResultCallback() {
        override fun onCompleted(gestureDescription: GestureDescription?) {
          completed = true
          latch.countDown()
        }

        override fun onCancelled(gestureDescription: GestureDescription?) {
          latch.countDown()
        }
      },
      handler,
    )
    if (!dispatched) return false
    latch.await(2, TimeUnit.SECONDS)
    return completed
  }
}
