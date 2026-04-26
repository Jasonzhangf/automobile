package com.flowy.explore.foundation

import java.security.MessageDigest
import org.json.JSONObject

object PageSignatureBuilder {
  fun build(
    pageContext: JSONObject,
    accessibilityRawJson: String?,
    hasScreenshot: Boolean,
  ): String {
    val app = pageContext.optJSONObject("app") ?: JSONObject()
    val screen = pageContext.optJSONObject("screen") ?: JSONObject()
    val runtime = pageContext.optJSONObject("runtime") ?: JSONObject()
    val payload = listOf(
      app.optString("packageName", ""),
      app.optString("windowTitle", ""),
      screen.optInt("widthPx").toString(),
      screen.optInt("heightPx").toString(),
      screen.optInt("rotation").toString(),
      runtime.optBoolean("projectionReady").toString(),
      runtime.optBoolean("accessibilityReady").toString(),
      hasScreenshot.toString(),
      accessibilityRawJson?.hashCode()?.toString().orEmpty(),
    ).joinToString("|")
    return MessageDigest.getInstance("SHA-256")
      .digest(payload.toByteArray())
      .joinToString("") { "%02x".format(it) }
      .take(12)
  }
}
