package com.flowy.explore.foundation

import android.content.Context

class UpgradeStateStore(context: Context) {
  private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

  fun savePending(version: String, manifestUrl: String) {
    prefs.edit().putString(KEY_VERSION, version).putString(KEY_MANIFEST_URL, manifestUrl).apply()
  }

  fun pendingVersion(): String? = prefs.getString(KEY_VERSION, null)

  fun pendingManifestUrl(): String? = prefs.getString(KEY_MANIFEST_URL, null)

  fun clear() {
    prefs.edit().remove(KEY_VERSION).remove(KEY_MANIFEST_URL).apply()
  }

  companion object {
    private const val PREFS_NAME = "flowy_upgrade_state"
    private const val KEY_VERSION = "pending_version"
    private const val KEY_MANIFEST_URL = "pending_manifest_url"
  }
}
