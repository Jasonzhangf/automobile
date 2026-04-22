package com.flowy.explore.runtime

data class LogEntry(
  val ts: String,
  val level: String,
  val event: String,
  val requestId: String? = null,
  val runId: String? = null,
  val command: String? = null,
  val message: String,
)
