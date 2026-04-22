package com.flowy.explore.foundation

import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object TimeHelper {
  private val formatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME

  fun now(): String = OffsetDateTime.now(ZoneId.systemDefault()).format(formatter)
}
