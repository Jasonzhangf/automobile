package com.flowy.explore.blocks

import com.flowy.explore.runtime.LocalLogStore
import com.flowy.explore.runtime.LogEntry

class ReadLogTailBlock(private val logStore: LocalLogStore) {
  fun read(limit: Int): List<LogEntry> = logStore.tail(limit)
}
