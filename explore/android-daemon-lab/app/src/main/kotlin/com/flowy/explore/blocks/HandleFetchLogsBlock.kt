package com.flowy.explore.blocks

import android.os.Build
import com.flowy.explore.foundation.TimeHelper
import com.flowy.explore.foundation.VersionReader
import com.flowy.explore.runtime.LogEntry
import org.json.JSONArray
import org.json.JSONObject

class HandleFetchLogsBlock(
  private val versionReader: VersionReader,
  private val readLogTailBlock: ReadLogTailBlock,
) {
  fun run(requestId: String, runId: String, command: String, tail: Int): JSONObject {
    val logs = readLogTailBlock.read(tail)
    val now = TimeHelper.now()
    return JSONObject().apply {
      put("protocolVersion", "exp01")
      put("requestId", requestId)
      put("runId", runId)
      put("command", command)
      put("status", "ok")
      put("startedAt", now)
      put("finishedAt", now)
      put("durationMs", 0)
      put("device", JSONObject().apply {
        put("deviceId", Build.DEVICE)
        put("model", Build.MODEL)
        put("androidVersion", Build.VERSION.RELEASE)
      })
      put("app", JSONObject().apply {
        put("packageName", "com.flowy.explore")
        put("runtimeVersion", versionReader.versionName())
      })
      put("artifacts", JSONArray())
      put("error", JSONObject.NULL)
      put("message", "logs")
      put("inlineLogs", logs.toJsonArray())
    }
  }

  private fun List<LogEntry>.toJsonArray(): JSONArray = JSONArray().apply {
    forEach { entry ->
      put(JSONObject().apply {
        put("ts", entry.ts)
        put("level", entry.level)
        put("event", entry.event)
        entry.requestId?.let { put("requestId", it) }
        entry.runId?.let { put("runId", it) }
        entry.command?.let { put("command", it) }
        put("message", entry.message)
      })
    }
  }
}
