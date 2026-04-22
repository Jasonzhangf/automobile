package com.flowy.explore.foundation

import android.content.Context

class DevServerOverrideStore(context: Context) {
  private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

  fun hostOverride(): String? = prefs.getString(KEY_HOST, null)?.takeIf { it.isNotBlank() }

  fun portOverride(): Int? {
    val value = prefs.getInt(KEY_PORT, -1)
    return value.takeIf { it > 0 }
  }

  fun saveHost(host: String) {
    prefs.edit().putString(KEY_HOST, host.trim()).apply()
  }

  fun savePort(port: Int?) {
    prefs.edit().apply {
      if (port == null || port <= 0) remove(KEY_PORT) else putInt(KEY_PORT, port)
    }.apply()
  }

  companion object {
    private const val PREFS_NAME = "flowy_dev_server_overrides"
    private const val KEY_HOST = "host"
    private const val KEY_PORT = "port"
  }
}
