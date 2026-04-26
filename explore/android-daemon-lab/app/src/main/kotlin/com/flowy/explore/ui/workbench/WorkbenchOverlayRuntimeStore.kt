package com.flowy.explore.ui.workbench

import android.content.Context

class WorkbenchOverlayRuntimeStore(context: Context) {
  private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

  fun isShowing(): Boolean = prefs.getBoolean(KEY_SHOWING, false)

  fun setShowing(showing: Boolean) {
    prefs.edit().putBoolean(KEY_SHOWING, showing).apply()
  }

  fun isExpanded(): Boolean = prefs.getBoolean(KEY_EXPANDED, false)

  fun setExpanded(expanded: Boolean) {
    prefs.edit().putBoolean(KEY_EXPANDED, expanded).apply()
  }

  companion object {
    private const val PREFS_NAME = "flowy_workbench_runtime"
    private const val KEY_SHOWING = "showing"
    private const val KEY_EXPANDED = "expanded"
  }
}
