package com.flowy.explore.foundation

import android.content.Context
import org.json.JSONObject

data class DevServerConfig(
  val host: String,
  val port: Int,
  val wsPath: String,
  val artifactPath: String,
  val upgradeCheckPath: String,
) {
  fun httpBaseUrl(): String = "http://$host:$port"

  fun wsUrl(): String = "ws://$host:$port$wsPath"

  fun artifactUrl(): String = "${httpBaseUrl()}$artifactPath"

  fun upgradeCheckUrl(): String = "${httpBaseUrl()}$upgradeCheckPath"
}

class DevServerReader(private val context: Context) {
  private val overrideStore = DevServerOverrideStore(context)

  fun read(): DevServerConfig {
    val raw = context.assets.open("dev-server.json").bufferedReader().use { it.readText() }
    val json = JSONObject(raw)
    return DevServerConfig(
      host = overrideStore.hostOverride() ?: json.getString("host"),
      port = overrideStore.portOverride() ?: json.getInt("port"),
      wsPath = json.getString("wsPath"),
      artifactPath = json.optString("artifactPath", "/exp01/artifacts"),
      upgradeCheckPath = json.optString("upgradeCheckPath", "/flowy/upgrade/check"),
    )
  }
}
