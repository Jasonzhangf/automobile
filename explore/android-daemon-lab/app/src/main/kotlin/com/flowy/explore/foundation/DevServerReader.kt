package com.flowy.explore.foundation

import android.content.Context
import org.json.JSONObject

data class DevServerConfig(
  val host: String,
  val port: Int,
  val wsPath: String,
  val artifactPath: String,
) {
  fun wsUrl(): String = "ws://$host:$port$wsPath"

  fun artifactUrl(): String = "http://$host:$port$artifactPath"
}

class DevServerReader(private val context: Context) {
  fun read(): DevServerConfig {
    val raw = context.assets.open("dev-server.json").bufferedReader().use { it.readText() }
    val json = JSONObject(raw)
    return DevServerConfig(
      host = json.getString("host"),
      port = json.getInt("port"),
      wsPath = json.getString("wsPath"),
      artifactPath = json.optString("artifactPath", "/exp01/artifacts"),
    )
  }
}
