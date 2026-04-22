package com.flowy.explore.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.flowy.explore.R
import com.flowy.explore.blocks.OpenAccessibilitySettingsBlock
import com.flowy.explore.blocks.OpenOverlayPermissionSettingsBlock
import com.flowy.explore.blocks.RequestProjectionPermissionBlock
import com.flowy.explore.blocks.StartDaemonBlock
import com.flowy.explore.blocks.StartOverlayWorkbenchBlock
import com.flowy.explore.blocks.StopDaemonBlock
import com.flowy.explore.blocks.StopOverlayWorkbenchBlock
import com.flowy.explore.foundation.AccessibilityStatusReader
import com.flowy.explore.foundation.DevServerReader
import com.flowy.explore.foundation.OverlayPermissionReader
import com.flowy.explore.foundation.VersionReader
import com.flowy.explore.runtime.DaemonForegroundService
import com.flowy.explore.runtime.MediaProjectionSessionHolder
import com.flowy.explore.ui.workbench.WorkbenchOverlayService

class DevPanelActivity : AppCompatActivity() {
  private lateinit var accessibilityStatusReader: AccessibilityStatusReader
  private lateinit var overlayPermissionReader: OverlayPermissionReader

  private val projectionPermissionLauncher = registerForActivityResult(
    ActivityResultContracts.StartActivityForResult(),
  ) { result ->
    MediaProjectionSessionHolder.store(result.resultCode, result.data)
    toast(
      if (MediaProjectionSessionHolder.isReady()) {
        "Screenshot permission granted"
      } else {
        "Screenshot permission not granted"
      },
    )
    renderRuntimeState()
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_dev_panel)

    val versionReader = VersionReader(this)
    val server = DevServerReader(this).read()
    accessibilityStatusReader = AccessibilityStatusReader(this)
    overlayPermissionReader = OverlayPermissionReader(this)
    findViewById<TextView>(R.id.textVersion).text = "Version: ${versionReader.versionName()}"
    findViewById<TextView>(R.id.textServer).text = "Server: ${server.wsUrl()}"
    findViewById<Button>(R.id.buttonStart).setOnClickListener { StartDaemonBlock(this).run() }
    findViewById<Button>(R.id.buttonStop).setOnClickListener { StopDaemonBlock(this).run() }
    findViewById<Button>(R.id.buttonOpenAccessibility).setOnClickListener {
      OpenAccessibilitySettingsBlock(this).run()
    }
    findViewById<Button>(R.id.buttonGrantProjection).setOnClickListener {
      projectionPermissionLauncher.launch(RequestProjectionPermissionBlock(this).createIntent())
    }
    findViewById<Button>(R.id.buttonGrantOverlay).setOnClickListener {
      OpenOverlayPermissionSettingsBlock(this).run()
    }
    findViewById<Button>(R.id.buttonOpenWorkbench).setOnClickListener {
      if (!overlayPermissionReader.isGranted()) {
        toast("overlay permission missing")
      } else {
        StartOverlayWorkbenchBlock(this).run()
        toast("workbench opened")
      }
    }
    findViewById<Button>(R.id.buttonCloseWorkbench).setOnClickListener {
      StopOverlayWorkbenchBlock(this).run()
      toast("workbench closed")
    }
    handleIntent(intent)
    renderRuntimeState()
  }

  override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    setIntent(intent)
    handleIntent(intent)
  }

  override fun onResume() {
    super.onResume()
    renderRuntimeState()
  }

  private fun handleIntent(intent: Intent?) {
    if (intent?.getBooleanExtra(EXTRA_REQUEST_PROJECTION, false) == true) {
      projectionPermissionLauncher.launch(RequestProjectionPermissionBlock(this).createIntent())
      intent.removeExtra(EXTRA_REQUEST_PROJECTION)
    }
  }

  private fun renderRuntimeState() {
    findViewById<TextView>(R.id.textStatus).text = "Status: ${DaemonForegroundService.currentStatus}"
    findViewById<TextView>(R.id.textHeartbeat).text = "Last heartbeat: ${DaemonForegroundService.lastHeartbeat}"
    findViewById<TextView>(R.id.textProjectionStatus).text =
      "Projection: ${MediaProjectionSessionHolder.statusText()}"
    findViewById<TextView>(R.id.textAccessibilityStatus).text =
      "Accessibility: ${accessibilityStatusReader.statusText()}"
    findViewById<TextView>(R.id.textOverlayStatus).text =
      "Overlay: ${overlayPermissionReader.statusText()} / showing=${WorkbenchOverlayService.isShowing}"
  }

  private fun toast(message: String) {
    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
  }

  companion object {
    const val EXTRA_REQUEST_PROJECTION = "request_projection"
  }
}
