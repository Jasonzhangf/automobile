package com.flowy.explore.runtime

import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File

class LocalLogStoreTest {
  @Test
  fun logEntryDataClassCarriesFields() {
    val entry = LogEntry("ts", "info", "event", "req", "run", "ping", "hello")
    assertEquals("hello", entry.message)
  }
}
