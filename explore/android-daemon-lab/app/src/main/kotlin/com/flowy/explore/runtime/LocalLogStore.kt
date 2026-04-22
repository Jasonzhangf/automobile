package com.flowy.explore.runtime

import android.content.Context
import com.flowy.explore.foundation.FileHelper
import com.flowy.explore.foundation.TimeHelper
import org.json.JSONObject
import java.io.File

class LocalLogStore(context: Context) {
  private val file = FileHelper.ensureParent(File(context.filesDir, "logs/daemon.log"))

  fun append(
    level: String,
    event: String,
    message: String,
    requestId: String? = null,
    runId: String? = null,
    command: String? = null,
  ) {
    val entry = LogEntry(TimeHelper.now(), level, event, requestId, runId, command, message)
    file.appendText(toJson(entry).toString() + "\n")
  }

  fun tail(limit: Int): List<LogEntry> {
    if (!file.exists()) return emptyList()
    return file.readLines().takeLast(limit).mapNotNull { line ->
      runCatching {
        val json = JSONObject(line)
        LogEntry(
          ts = json.getString("ts"),
          level = json.getString("level"),
          event = json.getString("event"),
          requestId = json.optString("requestId").ifBlank { null },
          runId = json.optString("runId").ifBlank { null },
          command = json.optString("command").ifBlank { null },
          message = json.getString("message"),
        )
      }.getOrNull()
    }
  }

  private fun toJson(entry: LogEntry): JSONObject = JSONObject().apply {
    put("ts", entry.ts)
    put("level", entry.level)
    put("event", entry.event)
    entry.requestId?.let { put("requestId", it) }
    entry.runId?.let { put("runId", it) }
    entry.command?.let { put("command", it) }
    put("message", entry.message)
  }
}
