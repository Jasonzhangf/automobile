package com.flowy.explore.blocks

import android.os.Build
import com.flowy.explore.foundation.TimeHelper
import com.flowy.explore.foundation.VersionReader
import org.json.JSONArray
import org.json.JSONObject

class HandlePingBlock(private val versionReader: VersionReader) {
  fun run(requestId: String, runId: String, command: String): JSONObject {
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
      put("message", "pong")
    }
  }
}
