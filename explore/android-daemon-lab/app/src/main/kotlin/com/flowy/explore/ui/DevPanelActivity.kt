package com.flowy.explore.ui

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.flowy.explore.R
import com.flowy.explore.blocks.OpenAccessibilitySettingsBlock
import com.flowy.explore.blocks.RequestProjectionPermissionBlock
import com.flowy.explore.blocks.StartDaemonBlock
import com.flowy.explore.blocks.StopDaemonBlock
import com.flowy.explore.foundation.AccessibilityStatusReader
import com.flowy.explore.foundation.DevServerReader
import com.flowy.explore.foundation.VersionReader
import com.flowy.explore.runtime.DaemonForegroundService
import com.flowy.explore.runtime.MediaProjectionSessionHolder

class DevPanelActivity : AppCompatActivity() {
  private lateinit var accessibilityStatusReader: AccessibilityStatusReader

  private val projectionPermissionLauncher = registerForActivityResult(
    ActivityResultContracts.StartActivityForResult(),
  ) { result ->
    MediaProjectionSessionHolder.store(result.resultCode, result.data)
    if (MediaProjectionSessionHolder.isReady()) {
      Toast.makeText(this, "Screenshot permission granted", Toast.LENGTH_SHORT).show()
    } else {
      Toast.makeText(this, "Screenshot permission not granted", Toast.LENGTH_SHORT).show()
    }
    renderRuntimeState()
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_dev_panel)

    val versionReader = VersionReader(this)
    val server = DevServerReader(this).read()
    accessibilityStatusReader = AccessibilityStatusReader(this)
    findViewById<TextView>(R.id.textVersion).text = "Version: ${versionReader.versionName()}"
    findViewById<TextView>(R.id.textServer).text = "Server: ${server.wsUrl()}"
    findViewById<Button>(R.id.buttonStart).setOnClickListener { StartDaemonBlock(this).run() }
    findViewById<Button>(R.id.buttonStop).setOnClickListener { StopDaemonBlock(this).run() }
    findViewById<Button>(R.id.buttonOpenAccessibility).setOnClickListener {
      OpenAccessibilitySettingsBlock(this).run()
    }
    findViewById<Button>(R.id.buttonGrantProjection).setOnClickListener {
      val intent = RequestProjectionPermissionBlock(this).createIntent()
      projectionPermissionLauncher.launch(intent)
    }
    renderRuntimeState()
  }

  override fun onResume() {
    super.onResume()
    renderRuntimeState()
  }

  private fun renderRuntimeState() {
    findViewById<TextView>(R.id.textStatus).text = "Status: ${DaemonForegroundService.currentStatus}"
    findViewById<TextView>(R.id.textHeartbeat).text = "Last heartbeat: ${DaemonForegroundService.lastHeartbeat}"
    findViewById<TextView>(R.id.textProjectionStatus).text =
      "Projection: ${MediaProjectionSessionHolder.statusText()}"
    findViewById<TextView>(R.id.textAccessibilityStatus).text =
      "Accessibility: ${accessibilityStatusReader.statusText()}"
  }
}
