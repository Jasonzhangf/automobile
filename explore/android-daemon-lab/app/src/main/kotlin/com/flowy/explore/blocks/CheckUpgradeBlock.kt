package com.flowy.explore.blocks

import com.flowy.explore.foundation.DevServerConfig
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

data class UpgradeCheckResult(
  val available: Boolean,
  val latestVersion: String,
  val manifestUrl: String,
)

class CheckUpgradeBlock(private val client: OkHttpClient = OkHttpClient()) {
  fun run(config: DevServerConfig, currentVersion: String): UpgradeCheckResult {
    val request = Request.Builder()
      .url("${config.upgradeCheckUrl()}?currentVersion=$currentVersion")
      .get()
      .build()
    client.newCall(request).execute().use { response ->
      if (!response.isSuccessful) error("upgrade check failed: ${response.code}")
      val body = response.body?.string() ?: error("missing response body")
      val json = JSONObject(body)
      return UpgradeCheckResult(
        available = json.optBoolean("available", false),
        latestVersion = json.optString("latestVersion"),
        manifestUrl = json.optString("manifestUrl"),
      )
    }
  }
}
