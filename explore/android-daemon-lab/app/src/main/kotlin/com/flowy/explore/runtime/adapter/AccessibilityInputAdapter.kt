package com.flowy.explore.runtime.adapter

import com.flowy.explore.foundation.executor.InputExecutor
import com.flowy.explore.runtime.FlowyAccessibilityService

object AccessibilityInputAdapter : InputExecutor {
  override fun inputText(text: String): Boolean {
    val svc = FlowyAccessibilityService.requireInstance()
    return svc.performInputText(text)
  }

  override fun pressKey(keyCode: Int): Boolean {
    val svc = FlowyAccessibilityService.requireInstance()
    return svc.performPressKey(keyCode)
  }
}
