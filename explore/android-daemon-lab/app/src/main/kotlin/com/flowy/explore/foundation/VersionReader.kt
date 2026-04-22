package com.flowy.explore.foundation

import android.content.Context
import org.json.JSONObject

class VersionReader(private val context: Context) {
  fun versionName(): String = json().getString("versionName")

  fun buildNumber(): Int = json().getInt("buildNumber")

  private fun json(): JSONObject {
    val raw = context.assets.open("runtime-version.json").bufferedReader().use { it.readText() }
    return JSONObject(raw)
  }
}
