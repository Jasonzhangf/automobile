package com.flowy.explore.runtime

import android.app.Activity
import android.content.Intent
import com.flowy.explore.foundation.TimeHelper

object MediaProjectionSessionHolder {
  @Volatile private var projectionResultCode: Int? = null
  @Volatile private var projectionData: Intent? = null
  @Volatile private var grantedAt: String? = null

  fun store(resultCode: Int, data: Intent?) {
    if (resultCode != Activity.RESULT_OK || data == null) {
      clear()
      return
    }
    projectionResultCode = resultCode
    projectionData = Intent(data)
    grantedAt = TimeHelper.now()
  }

  fun clear() {
    projectionResultCode = null
    projectionData = null
    grantedAt = null
  }

  fun isReady(): Boolean = projectionResultCode == Activity.RESULT_OK && projectionData != null

  fun statusText(): String {
    val grantedAtValue = grantedAt
    return if (isReady()) {
      "ready${grantedAtValue?.let { " @ $it" } ?: ""}"
    } else {
      "not-ready"
    }
  }

  fun resultCode(): Int? = projectionResultCode

  fun dataIntent(): Intent? = projectionData?.let(::Intent)
}
