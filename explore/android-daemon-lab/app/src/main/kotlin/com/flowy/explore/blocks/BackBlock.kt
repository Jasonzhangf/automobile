package com.flowy.explore.blocks

import com.flowy.explore.runtime.FlowyAccessibilityService

class BackBlock {
  fun run(): String {
    check(FlowyAccessibilityService.requireInstance().performBack()) { "BACK_FAILED" }
    return "back"
  }
}
