package com.flowy.explore.foundation.executor

interface ScrollExecutor {
  fun scroll(x: Int, y: Int, direction: String): Boolean
}
