package com.flowy.explore.blocks

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import org.json.JSONObject

class OpenDeepLinkBlock(
  private val launch: (String, String?, String?) -> Unit,
) {
  constructor(context: Context) : this(
    launch = { uri, packageName, component ->
      val intent = if (uri.isBlank()) {
        when {
          !component.isNullOrBlank() -> {
            val resolvedComponent = ComponentName.unflattenFromString(component) ?: error("INVALID_COMPONENT")
            Intent(Intent.ACTION_MAIN).apply {
              addCategory(Intent.CATEGORY_LAUNCHER)
              this.component = resolvedComponent
            }
          }
          else -> {
            val resolvedPackage = packageName ?: error("PACKAGE_NAME_REQUIRED")
            val matches = context.packageManager.queryIntentActivities(
              Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
                setPackage(resolvedPackage)
              },
              0,
            )
            val activity = matches.firstOrNull()?.activityInfo ?: error("PACKAGE_NOT_FOUND")
            Intent(Intent.ACTION_MAIN).apply {
              addCategory(Intent.CATEGORY_LAUNCHER)
              this.component = ComponentName(activity.packageName, activity.name)
            }
          }
        }
      } else {
        Intent(Intent.ACTION_VIEW, Uri.parse(uri)).apply {
          packageName?.takeIf { it.isNotBlank() }?.let { setPackage(it) }
        }
      }
      context.startActivity(intent.apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) })
    },
  )

  fun run(payload: JSONObject): String {
    val uri = payload.optString("uri")
    val packageName = payload.optString("packageName").ifBlank { null }
    val component = payload.optString("component").ifBlank { null }
    if (uri.isBlank() && packageName == null && component == null) error("URI_PACKAGE_OR_COMPONENT_REQUIRED")
    launch(uri, packageName, component)
    return when {
      uri.isNotBlank() && packageName != null -> "open-deep-link:$uri#$packageName"
      uri.isNotBlank() -> "open-deep-link:$uri"
      component != null -> "open-component:$component"
      else -> "open-app:$packageName"
    }
  }
}
