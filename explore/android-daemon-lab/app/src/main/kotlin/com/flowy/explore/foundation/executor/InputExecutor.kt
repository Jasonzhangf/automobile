package com.flowy.explore.foundation.executor

interface InputExecutor {
  fun inputText(text: String): Boolean
  fun pressKey(keyCode: Int): Boolean
}
