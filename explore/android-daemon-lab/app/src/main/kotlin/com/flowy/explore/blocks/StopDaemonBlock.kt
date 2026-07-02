package com.flowy.explore.blocks

import com.flowy.explore.foundation.executor.ServiceController
import org.json.JSONObject

class StopDaemonBlock(
  private val serviceController: ServiceController,
) {
  fun run(payload: JSONObject): String {
    check(serviceController.stop()) { "STOP_DAEMON_FAILED" }
    return "daemon-stopped"
  }
}
