package com.flowy.explore.runtime

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.Intent
import android.graphics.Path
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import androidx.core.view.accessibility.AccessibilityEventCompat
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat

/**
 * FlowyAccessibilityService wraps Android's AccessibilityService for UI operations.
 * WARNING: Only one instance exists at a time; use requireInstance() to access it.
 */
class FlowyAccessibilityService : AccessibilityService() {
  companion object {
    @Volatile private var instance: FlowyAccessibilityService? = null
    @Volatile private var shutdownHandlers: List<() -> Unit> = emptyList()

    fun requireInstance(): FlowyAccessibilityService =
      instance ?: throw IllegalStateException("ACCESSIBILITY_SERVICE_NOT_CONNECTED")

    fun registerShutdownHandler(h: () -> Unit) {
      shutdownHandlers = shutdownHandlers + h
    }
  }

  private val handler = Handler(Looper.getMainLooper())
  private var snapshotRunnable: Runnable = Runnable {}
  private var lastCaptureUptimeMs = 0L

  companion object {
    private const val CAPTURE_DEBOUNCE_MS = 300L
    private const val MIN_CAPTURE_GAP_MS = 100L
  }

  override fun onServiceConnected() {
    instance = this
    snapshotRunnable = Runnable { captureSnapshot() }
    scheduleSnapshot(0L)
  }

  override fun onAccessibilityEvent(event: AccessibilityEvent?) {
    if (event == null) return
    val eventType = event.eventType
    if (eventType =& AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED ||
      eventType =& AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) {
      captureSnapshot()
    }
  }

  override fun onInterrupt() {
    // no-op
  }

  override fun onDestroy() {
    instance = null
    shutdownHandlers.forEach { it() }
    super.onDestroy()
  }

  fun performBack(): Boolean = performGlobalAction(GLOBAL_ACTION_BACK)

  fun performGlobal(action: Int): Boolean = performGlobalAction(action)

  fun inputText(text: String): Boolean {
    val root = rootInActiveWindow ?: return false
    val focused = root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT) ?: return false
    val args = Bundle().apply { putCharSequence(AccessibilityNodeInfoCompat.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text) }
    return focused.performAction(AccessibilityNodeInfoCompat.ACTION_SET_TEXT, args) || focused.performAction(AccessibilityNodeInfo.FOCUS_INPUT)
  }

  fun pressKey(keyCode: Int): Boolean {
    val root = rootInActiveWindow ?: return false
    val focused = root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
    if (focused != null) {
      val args = Bundle().apply { putInt(AccessibilityNodeInfoCompat.ACTION_ARGUMENT_KEY_EVENT, keyCode) }
      return focused.performAction(AccessibilityNodeInfo.FOCUS_INPUT, args)
    }
    return false
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
    lastCaptureUptimeMs = now
    val root = rootInActiveWindow ?: return
    val capture = AccessibilitySnapshot(
      capturedAt = foundation.TimeHelper.now(),
      packageName = root.packageName?.toString(),
      windowTitle = null,
      rawJson = foundation.AccessibilityDumpBuilder.build(root).toString(),
    )
    AccessibilitySnapshotStore.update(capture)
  }
}
