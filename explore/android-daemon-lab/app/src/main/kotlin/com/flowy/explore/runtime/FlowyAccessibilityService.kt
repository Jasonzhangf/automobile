package com.flowy.explore.runtime

import android.accessibilityservice.AccessibilityService
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.accessibility.AccessibilityEvent
import com.flowy.explore.foundation.AccessibilityTreeSerializer

class FlowyAccessibilityService : AccessibilityService() {
  private val serializer = AccessibilityTreeSerializer()
  private val handler = Handler(Looper.getMainLooper())
  private val snapshotRunnable = Runnable { captureSnapshot() }
  private var lastCaptureUptimeMs: Long = 0L

  override fun onServiceConnected() {
    super.onServiceConnected()
    scheduleSnapshot(initialDelayMs = 200L)
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
    super.onDestroy()
  }

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
  }
}
