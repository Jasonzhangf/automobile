package com.flowy.explore.foundation

import android.accessibilityservice.AccessibilityServiceInfo
import android.view.accessibility.AccessibilityManager
import android.content.ComponentName
import android.content.Context
import android.provider.Settings

class AccessibilityStatusReader(private val context: Context) {
  fun isEnabled(): Boolean {
    if (isEnabledFromManager()) return true
    val enabled = Settings.Secure.getString(
      context.contentResolver,
      Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
    ) ?: return false
    val target = ComponentName(context, com.flowy.explore.runtime.FlowyAccessibilityService::class.java)
    val full = target.flattenToString()
    val short = target.flattenToShortString()
    return enabled.split(':').any { value ->
      value.equals(full, ignoreCase = true) || value.equals(short, ignoreCase = true)
    }
  }

  fun statusText(): String = if (isEnabled()) "enabled" else "disabled"

  private fun isEnabledFromManager(): Boolean {
    val manager = context.getSystemService(AccessibilityManager::class.java) ?: return false
    return manager.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK).any { info ->
      val serviceInfo = info.resolveInfo?.serviceInfo ?: return@any false
      serviceInfo.packageName == context.packageName &&
        serviceInfo.name == com.flowy.explore.runtime.FlowyAccessibilityService::class.java.name
    }
  }
}
