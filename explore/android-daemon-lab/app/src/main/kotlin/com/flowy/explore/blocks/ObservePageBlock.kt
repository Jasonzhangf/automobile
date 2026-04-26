package com.flowy.explore.blocks

import com.flowy.explore.foundation.BlockResultFactory
import com.flowy.explore.foundation.DisplayInfo
import com.flowy.explore.foundation.DisplayInfoReader
import com.flowy.explore.foundation.ObservedPageState
import com.flowy.explore.foundation.PageContextBuilder
import com.flowy.explore.foundation.PageSignatureBuilder
import com.flowy.explore.foundation.TimeHelper
import com.flowy.explore.runtime.AccessibilitySnapshot
import com.flowy.explore.runtime.AccessibilitySnapshotStore
import com.flowy.explore.runtime.MediaProjectionSessionHolder
import com.flowy.explore.runtime.ProjectionCapture
import org.json.JSONObject

class ObservePageBlock(
  private val readDisplayInfo: () -> DisplayInfo,
  private val dumpAccessibility: () -> AccessibilitySnapshot?,
  private val dumpRootUi: () -> AccessibilitySnapshot?,
  private val peekAccessibility: () -> AccessibilitySnapshot?,
  private val captureScreenshot: () -> ProjectionCapture,
  private val isProjectionReady: () -> Boolean,
  private val now: () -> String = TimeHelper::now,
) {
  constructor(
    displayInfoReader: DisplayInfoReader,
    dumpAccessibilityTreeBlock: DumpAccessibilityTreeBlock,
    dumpUiTreeRootBlock: DumpUiTreeRootBlock,
    captureScreenshotBlock: CaptureScreenshotBlock,
  ) : this(
    readDisplayInfo = displayInfoReader::read,
    dumpAccessibility = dumpAccessibilityTreeBlock::run,
    dumpRootUi = dumpUiTreeRootBlock::run,
    peekAccessibility = AccessibilitySnapshotStore::current,
    captureScreenshot = captureScreenshotBlock::run,
    isProjectionReady = MediaProjectionSessionHolder::isReady,
  )

  fun observe(
    requestId: String,
    runId: String,
    command: String,
    observerSpec: JSONObject,
  ): ObservedPageState {
    val requireAccessibility = observerSpec.optBoolean("requireAccessibility")
    val requireRootDump = observerSpec.optBoolean("requireRootDump")
    val requireScreenshot = observerSpec.optBoolean("requireScreenshot")
    val accessibilitySnapshot = when {
      requireRootDump -> dumpRootUi() ?: error("ROOT_UI_DUMP_UNAVAILABLE")
      requireAccessibility -> dumpAccessibility() ?: error("ACCESSIBILITY_ROOT_UNAVAILABLE")
      else -> peekAccessibility()
    }
    val screenshotCapture = if (requireScreenshot) captureScreenshot() else null
    val capturedAt = accessibilitySnapshot?.capturedAt ?: now()
    val displayInfo = screenshotCapture?.displayInfo ?: readDisplayInfo()
    val pageContext = PageContextBuilder.build(
      requestId = requestId,
      runId = runId,
      command = command,
      capturedAt = capturedAt,
      displayInfo = displayInfo,
      projectionReady = screenshotCapture != null || isProjectionReady(),
      accessibilitySnapshot = accessibilitySnapshot,
    )
    return ObservedPageState(
      pageContext = pageContext,
      pageSignature = PageSignatureBuilder.build(pageContext, accessibilitySnapshot?.rawJson, screenshotCapture != null),
      displayInfo = displayInfo,
      accessibilitySnapshot = accessibilitySnapshot,
      screenshotCapture = screenshotCapture,
    )
  }

  fun run(requestId: String, runId: String, command: String, observerSpec: JSONObject): JSONObject {
    val startedAt = now()
    return try {
      val observedPage = observe(requestId, runId, command, observerSpec)
      BlockResultFactory.ok(
        startedAt = startedAt,
        output = JSONObject().apply {
          put("pageStateRef", "page-state:current")
          put("pageSignature", observedPage.pageSignature)
        },
      )
    } catch (throwable: Throwable) {
      val code = throwable.message ?: "OBSERVE_PAGE_FAILED"
      BlockResultFactory.error(startedAt = startedAt, code = code, message = code)
    }
  }
}
