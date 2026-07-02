package com.flowy.explore.blocks

import com.flowy.explore.foundation.executor.ServiceController
import org.json.JSONObject

class StartDaemonBlock(
  private val serviceController: ServiceController,
) {
  fun run(payload: JSONObject): String {
    check(serviceController.start()) { "START_DAEMON_FAILED" }
    return "daemon-started"
  }
}
