package com.flowy.explore.ui.workbench

import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import com.flowy.explore.blocks.OpenAccessibilitySettingsBlock
import com.flowy.explore.blocks.OpenOverlayPermissionSettingsBlock
import com.flowy.explore.blocks.StartDaemonBlock
import com.flowy.explore.blocks.StopDaemonBlock
import com.flowy.explore.foundation.AgentMode
import com.flowy.explore.foundation.DevServerOverrideStore
import com.flowy.explore.foundation.WorkbenchPreferenceStore
import com.flowy.explore.ui.DevPanelActivity
import kotlin.math.max

class WorkbenchOverlayService : Service() {
  private val handler = Handler(Looper.getMainLooper())
  private lateinit var windowManager: WindowManager
  private lateinit var stateStore: OverlayUiStateStore
  private lateinit var prefs: WorkbenchPreferenceStore
  private lateinit var overrideStore: DevServerOverrideStore
  private lateinit var viewFactory: WorkbenchViewFactory
  private lateinit var views: WorkbenchViewFactory.Views
  private lateinit var bubbleParams: WindowManager.LayoutParams
  private lateinit var panelParams: WindowManager.LayoutParams
  private var expanded = false
  private var currentSection = Section.AGENT_CONTROL

  private val renderLoop = object : Runnable {
    override fun run() {
      if (::views.isInitialized) renderState()
      handler.postDelayed(this, 1000L)
    }
  }

  override fun onCreate() {
    super.onCreate()
    isShowing = true
    windowManager = getSystemService(WindowManager::class.java)
    stateStore = OverlayUiStateStore(this)
    prefs = WorkbenchPreferenceStore(this)
    overrideStore = DevServerOverrideStore(this)
    viewFactory = WorkbenchViewFactory(this)
    views = viewFactory.createViews()
    bubbleParams = bubbleParams()
    panelParams = panelParams()
    attachViews()
    bindInteractions()
    renderState()
    handler.post(renderLoop)
  }

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    when (intent?.action) {
      ACTION_HIDE -> stopSelf()
      ACTION_SHOW -> renderState()
    }
    return START_STICKY
  }

  override fun onDestroy() {
    handler.removeCallbacks(renderLoop)
    removeViewSafely(views.panelRoot)
    removeViewSafely(views.bubbleRoot)
    isShowing = false
    super.onDestroy()
  }

  override fun onBind(intent: Intent?): IBinder? = null

  private fun attachViews() {
    windowManager.addView(views.bubbleRoot, bubbleParams)
    windowManager.addView(views.panelRoot, panelParams)
    views.panelRoot.visibility = View.GONE
  }

  private fun bindInteractions() {
    val dragListener = BubbleDragListener(
      onTap = { toggleExpanded() },
      onMove = { rawX, rawY ->
        bubbleParams.x = rawX
        bubbleParams.y = rawY
        panelParams.y = rawY
        updatePositions()
      },
      onRelease = {
        bubbleParams.x = if (bubbleParams.x < 0) -screenWidth() / 2 + dp(34) else screenWidth() / 2 - dp(34)
        panelParams.y = bubbleParams.y
        updatePositions()
      },
    )
    views.bubbleRoot.setOnTouchListener(dragListener)
    views.agentControlButton.setOnClickListener {
      currentSection = Section.AGENT_CONTROL
      renderSection(stateStore.snapshot())
    }
    views.captureModeButton.setOnClickListener {
      currentSection = Section.CAPTURE_MODE
      renderSection(stateStore.snapshot())
    }
    views.systemSettingsButton.setOnClickListener {
      currentSection = Section.SYSTEM_SETTINGS
      renderSection(stateStore.snapshot())
    }
  }

  private fun toggleExpanded() {
    expanded = !expanded
    views.panelRoot.visibility = if (expanded) View.VISIBLE else View.GONE
    renderState()
  }

  private fun renderState() {
    val snapshot = stateStore.snapshot()
    views.bubbleRoot.text = OverlayStatusFormatter.bubbleLabel(snapshot)
    views.bubbleRoot.setBackgroundColor(OverlayStatusFormatter.bubbleStateColor(snapshot))
    views.statusTitle.text = "${snapshot.agentMode.name.lowercase()} · ${OverlayStatusFormatter.bubbleLabel(snapshot)}"
    views.statusBody.text = OverlayStatusFormatter.statusSummary(snapshot)
    renderSection(snapshot)
    updatePositions()
  }

  private fun renderSection(snapshot: WorkbenchStatusSnapshot) {
    views.contentHost.removeAllViews()
    val sectionView = when (currentSection) {
      Section.AGENT_CONTROL -> viewFactory.buildAgentControlSection(
        snapshot = snapshot,
        onPassiveMode = { prefs.setAgentMode(AgentMode.PASSIVE); renderState() },
        onActiveMode = { prefs.setAgentMode(AgentMode.ACTIVE); renderState() },
        onHostCommit = { host ->
          if (host.isNotBlank()) {
            overrideStore.saveHost(host)
            reconnectDaemon()
          }
        },
        onPortCommit = { portText ->
          overrideStore.savePort(portText.toIntOrNull())
          reconnectDaemon()
        },
      )
      Section.CAPTURE_MODE -> viewFactory.buildCaptureSection(
        snapshot = snapshot,
        onEnter = {
          prefs.setCaptureModeEnabled(true)
          toast("capture mode active")
          renderState()
        },
        onExit = {
          prefs.setCaptureModeEnabled(false)
          toast("capture mode inactive")
          renderState()
        },
      )
      Section.SYSTEM_SETTINGS -> viewFactory.buildSystemSection(
        snapshot = snapshot,
        onOpenOverlayPermission = { OpenOverlayPermissionSettingsBlock(this).run() },
        onOpenAccessibility = { OpenAccessibilitySettingsBlock(this).run() },
        onOpenDevPanel = {
          startActivity(Intent(this, DevPanelActivity::class.java).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) })
        },
        onCheckUpgrade = { toast("upgrade check skeleton ready") },
      )
    }
    views.contentHost.addView(sectionView)
  }

  private fun reconnectDaemon() {
    StopDaemonBlock(this).run()
    handler.postDelayed({ StartDaemonBlock(this).run() }, 250L)
    handler.postDelayed({ renderState() }, 400L)
  }

  private fun updatePositions() {
    val inwardOffset = if (bubbleParams.x < 0) dp(78) else -dp(250)
    panelParams.x = bubbleParams.x + inwardOffset
    if (::views.isInitialized) {
      windowManager.updateViewLayout(views.bubbleRoot, bubbleParams)
      windowManager.updateViewLayout(views.panelRoot, panelParams)
    }
  }

  private fun bubbleParams(): WindowManager.LayoutParams {
    return WindowManager.LayoutParams(
      WindowManager.LayoutParams.WRAP_CONTENT,
      WindowManager.LayoutParams.WRAP_CONTENT,
      overlayType(),
      WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
      PixelFormat.TRANSLUCENT,
    ).apply {
      gravity = Gravity.CENTER or Gravity.END
      x = screenWidth() / 2 - dp(34)
      y = 0
    }
  }

  private fun panelParams(): WindowManager.LayoutParams {
    return WindowManager.LayoutParams(
      dp(220),
      WindowManager.LayoutParams.WRAP_CONTENT,
      overlayType(),
      WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
      PixelFormat.TRANSLUCENT,
    ).apply {
      gravity = Gravity.CENTER or Gravity.END
      x = -dp(250)
      y = 0
    }
  }

  private fun overlayType(): Int {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
    } else {
      WindowManager.LayoutParams.TYPE_PHONE
    }
  }

  private fun screenWidth(): Int = resources.displayMetrics.widthPixels

  private fun dp(value: Int): Int = max(1, (value * resources.displayMetrics.density).toInt())

  private fun removeViewSafely(view: View) {
    runCatching { windowManager.removeView(view) }
  }

  private fun toast(message: String) {
    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
  }

  companion object {
    const val ACTION_SHOW = "com.flowy.explore.action.WORKBENCH_SHOW"
    const val ACTION_HIDE = "com.flowy.explore.action.WORKBENCH_HIDE"

    @Volatile var isShowing: Boolean = false
  }

  private enum class Section {
    AGENT_CONTROL,
    CAPTURE_MODE,
    SYSTEM_SETTINGS,
  }
}

private class BubbleDragListener(
  private val onTap: () -> Unit,
  private val onMove: (Int, Int) -> Unit,
  private val onRelease: () -> Unit,
) : View.OnTouchListener {
  private var downRawX = 0f
  private var downRawY = 0f
  private var moved = false

  override fun onTouch(view: View, event: MotionEvent): Boolean {
    when (event.actionMasked) {
      MotionEvent.ACTION_DOWN -> {
        downRawX = event.rawX
        downRawY = event.rawY
        moved = false
        return true
      }
      MotionEvent.ACTION_MOVE -> {
        if (kotlin.math.abs(event.rawX - downRawX) > 8 || kotlin.math.abs(event.rawY - downRawY) > 8) {
          moved = true
        }
        onMove(event.rawX.toInt() - view.width / 2, event.rawY.toInt() - view.height / 2)
        return true
      }
      MotionEvent.ACTION_UP -> {
        if (moved) onRelease() else onTap()
        return true
      }
    }
    return false
  }
}
