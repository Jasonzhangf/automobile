package com.flowy.explore.ui

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.flowy.explore.R
import com.flowy.explore.blocks.StartDaemonBlock
import com.flowy.explore.blocks.StartOverlayWorkbenchBlock
import com.flowy.explore.blocks.StopDaemonBlock
import com.flowy.explore.foundation.DevServerOverrideStore
import com.flowy.explore.foundation.DevServerReader

class DaemonConfigActivity : AppCompatActivity() {
  private val handler = Handler(Looper.getMainLooper())
  private lateinit var overrideStore: DevServerOverrideStore
  private lateinit var hostInput: EditText
  private lateinit var portInput: EditText
  private var overlayRestoreIssued = false

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_daemon_config)
    overrideStore = DevServerOverrideStore(this)
    val config = DevServerReader(this).read()
    findViewById<TextView>(R.id.textConfigCurrent).text = "当前: ${config.host}:${config.port}"
    hostInput = findViewById<EditText>(R.id.inputConfigHost).apply { setText(config.host) }
    portInput = findViewById<EditText>(R.id.inputConfigPort).apply { setText(config.port.toString()) }
    findViewById<Button>(R.id.buttonConfigCancel).setOnClickListener { finish() }
    findViewById<Button>(R.id.buttonConfigSave).setOnClickListener { saveAndReconnect() }
  }

  private fun saveAndReconnect() {
    val host = hostInput.text.toString().trim()
    val port = portInput.text.toString().trim().toIntOrNull()
    if (host.isBlank() || port == null || port <= 0) {
      toast("配置无效")
      return
    }
    overrideStore.saveHost(host)
    overrideStore.savePort(port)
    StopDaemonBlock(this).run()
    window.decorView.postDelayed({ StartDaemonBlock(this).run() }, 250L)
    toast("连接配置已更新")
    finish()
  }

  override fun finish() {
    super.finish()
    restoreOverlayIfNeeded()
  }

  private fun restoreOverlayIfNeeded() {
    if (overlayRestoreIssued || !intent.getBooleanExtra(EXTRA_RESTORE_OVERLAY, false)) return
    overlayRestoreIssued = true
    handler.postDelayed({ StartOverlayWorkbenchBlock(applicationContext).run() }, 180L)
  }

  private fun toast(message: String) {
    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
  }

  companion object {
    const val EXTRA_RESTORE_OVERLAY = "restore_overlay"
  }
}
