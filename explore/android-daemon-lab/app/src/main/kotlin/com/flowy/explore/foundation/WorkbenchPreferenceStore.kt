package com.flowy.explore.foundation

import android.content.Context

enum class AgentMode {
  PASSIVE,
  ACTIVE,
}

class WorkbenchPreferenceStore(context: Context) {
  private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

  fun agentMode(): AgentMode {
    return runCatching { AgentMode.valueOf(prefs.getString(KEY_AGENT_MODE, AgentMode.PASSIVE.name)!!) }
      .getOrDefault(AgentMode.PASSIVE)
  }

  fun setAgentMode(mode: AgentMode) {
    prefs.edit().putString(KEY_AGENT_MODE, mode.name).apply()
  }

  fun captureModeEnabled(): Boolean = prefs.getBoolean(KEY_CAPTURE_MODE_ENABLED, false)

  fun setCaptureModeEnabled(enabled: Boolean) {
    prefs.edit().putBoolean(KEY_CAPTURE_MODE_ENABLED, enabled).apply()
  }

  companion object {
    private const val PREFS_NAME = "flowy_workbench_prefs"
    private const val KEY_AGENT_MODE = "agent_mode"
    private const val KEY_CAPTURE_MODE_ENABLED = "capture_mode_enabled"
  }
}
