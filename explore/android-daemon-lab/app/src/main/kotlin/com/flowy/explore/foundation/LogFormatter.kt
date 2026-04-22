package com.flowy.explore.foundation

import com.flowy.explore.runtime.LogEntry

object LogFormatter {
  fun toLine(entry: LogEntry): String {
    return buildString {
      append(entry.ts)
      append(" [")
      append(entry.level)
      append("] ")
      append(entry.event)
      append(' ')
      append(entry.message)
    }
  }
}
