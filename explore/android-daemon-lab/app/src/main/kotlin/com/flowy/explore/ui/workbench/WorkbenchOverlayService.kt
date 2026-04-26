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
import com.flowy.explore.foundation.AgentMode
import com.flowy.explore.foundation.XhsSearchConfig
import com.flowy.explore.foundation.XhsSearchConfigStore
import com.flowy.explore.foundation.WorkbenchPreferenceStore
import com.flowy.explore.flows.UpgradeCheckFlow
import com.flowy.explore.ui.DaemonConfigActivity
import com.flowy.explore.ui.DevPanelActivity
import kotlin.math.max
import kotlin.math.min

class WorkbenchOverlayService : Service() {
  private val handler = Handler(Looper.getMainLooper())
  private lateinit var windowManager: WindowManager
  private lateinit var stateStore: OverlayUiStateStore
  private lateinit var runtimeStore: WorkbenchOverlayRuntimeStore
  private lateinit var prefs: WorkbenchPreferenceStore
  private lateinit var viewFactory: WorkbenchViewFactory
  private lateinit var views: WorkbenchViewFactory.Views
  private lateinit var upgradeCheckFlow: UpgradeCheckFlow
  private lateinit var xhsConfigStore: XhsSearchConfigStore
  private lateinit var bubbleParams: WindowManager.LayoutParams
  private lateinit var dismissParams: WindowManager.LayoutParams
  private lateinit var panelParams: WindowManager.LayoutParams
  private var expanded = false
  private var currentSection = Section.AGENT_CONTROL
  private var renderedSection: Section? = null
  private var renderedSectionKey: String? = null
  private var appsSubPage: AppsSubPage = AppsSubPage.LIST

  private val renderLoop = object : Runnable {
    override fun run() {
      if (::views.isInitialized) renderState()
      handler.postDelayed(this, 1000L)
    }
  }

  override fun onCreate() {
    super.onCreate()
    windowManager = getSystemService(WindowManager::class.java)
    stateStore = OverlayUiStateStore(this)
    runtimeStore = WorkbenchOverlayRuntimeStore(this)
    prefs = WorkbenchPreferenceStore(this)
    viewFactory = WorkbenchViewFactory(this)
    upgradeCheckFlow = UpgradeCheckFlow(this)
    xhsConfigStore = XhsSearchConfigStore(this)
    views = viewFactory.createViews()
    bubbleParams = bubbleParams()
    dismissParams = dismissParams()
    panelParams = panelParams()
    attachViews()
    bindInteractions()
    renderState()
    handler.post(renderLoop)
  }

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    when (intent?.action) {
      ACTION_HIDE -> stopSelf()
      ACTION_SHOW -> resumeOverlay()
      ACTION_SUSPEND -> suspendOverlay()
    }
    return START_STICKY
  }

  override fun onDestroy() {
    handler.removeCallbacks(renderLoop)
    removeViewSafely(views.panelRoot)
    removeViewSafely(views.dismissRoot)
    removeViewSafely(views.bubbleRoot)
    runtimeStore.setShowing(false)
    runtimeStore.setExpanded(false)
    isShowing = false
    super.onDestroy()
  }

  override fun onBind(intent: Intent?): IBinder? = null

  private fun attachViews() {
    windowManager.addView(views.dismissRoot, dismissParams)
    windowManager.addView(views.bubbleRoot, bubbleParams)
    windowManager.addView(views.panelRoot, panelParams)
    views.dismissRoot.visibility = View.GONE
    views.panelRoot.visibility = View.GONE
    runtimeStore.setShowing(true)
    runtimeStore.setExpanded(false)
    isShowing = true
  }

  private fun bindInteractions() {
    val dragListener = BubbleDragListener(
      onTap = { toggleExpanded() },
      onMove = { rawX, rawY ->
        bubbleParams.x = clampBubbleX(rawX)
        bubbleParams.y = clampBubbleY(rawY)
        updatePositions()
      },
      onRelease = {
        bubbleParams.x = snapBubbleToEdge(bubbleParams.x)
        updatePositions()
      },
    )
    views.bubbleRoot.setOnTouchListener(dragListener)
    views.dismissRoot.setOnClickListener { collapseExpanded() }
    views.agentControlButton.setOnClickListener {
      currentSection = Section.AGENT_CONTROL
      renderSection(stateStore.snapshot(), force = true)
    }
    views.captureModeButton.setOnClickListener {
      currentSection = Section.CAPTURE_MODE
      renderSection(stateStore.snapshot(), force = true)
    }
    views.systemSettingsButton.setOnClickListener {
      currentSection = Section.SYSTEM_SETTINGS
      renderSection(stateStore.snapshot(), force = true)
    }
    views.appsButton.setOnClickListener {
      currentSection = Section.APPS
      appsSubPage = AppsSubPage.LIST
      renderSection(stateStore.snapshot(), force = true)
    }
    views.upgradeButton.setOnClickListener {
      upgradeCheckFlow.check(
        onStatus = { handler.post { toast(it) } },
        onAvailable = { version -> handler.post { toast("发现新版本 $version，请点手动升级") } },
      )
    }
    views.manualUpgradeButton.setOnClickListener {
      upgradeCheckFlow.installPending {
        handler.post { toast(it) }
      }
    }
  }

  private fun toggleExpanded() {
    if (expanded) collapseExpanded() else showExpanded()
    renderState()
  }

  private fun showExpanded() {
    expanded = true
    views.dismissRoot.visibility = View.VISIBLE
    views.panelRoot.visibility = View.VISIBLE
    runtimeStore.setExpanded(true)
  }

  private fun collapseExpanded() {
    expanded = false
    views.dismissRoot.visibility = View.GONE
    views.panelRoot.visibility = View.GONE
    runtimeStore.setExpanded(false)
    renderState()
  }

  private fun suspendOverlay() {
    expanded = false
    views.dismissRoot.visibility = View.GONE
    views.panelRoot.visibility = View.GONE
    views.bubbleRoot.visibility = View.GONE
    runtimeStore.setExpanded(false)
    runtimeStore.setShowing(false)
    isShowing = false
  }

  private fun resumeOverlay() {
    views.bubbleRoot.visibility = View.VISIBLE
    if (expanded) {
      views.dismissRoot.visibility = View.VISIBLE
      views.panelRoot.visibility = View.VISIBLE
    }
    runtimeStore.setShowing(true)
    isShowing = true
    renderState()
  }

  private fun renderState() {
    val snapshot = stateStore.snapshot()
    val bubbleLabel = OverlayStatusFormatter.bubbleLabel(snapshot)
    val bubbleColor = OverlayStatusFormatter.bubbleStateColor(snapshot)
    views.bubbleRoot.render(bubbleLabel, bubbleColor)
    views.statusTitle.text = "${snapshot.agentMode.name.lowercase()} · $bubbleLabel"
    views.statusBody.text = OverlayStatusFormatter.statusSummary(snapshot)
    renderSection(snapshot)
    updatePositions()
  }

  private fun renderSection(snapshot: WorkbenchStatusSnapshot, force: Boolean = false) {
    val sectionKey = OverlaySectionRenderKey.build(currentSection, snapshot)
    if (!force && renderedSection == currentSection && renderedSectionKey == sectionKey) return
    views.contentHost.removeAllViews()
    val sectionView = when (currentSection) {
      Section.AGENT_CONTROL -> viewFactory.buildAgentControlSection(
        snapshot = snapshot,
        onPassiveMode = { prefs.setAgentMode(AgentMode.PASSIVE); renderState() },
        onActiveMode = { prefs.setAgentMode(AgentMode.ACTIVE); renderState() },
        onOpenConnectionConfig = { openConnectionConfig() },
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
      )
      Section.APPS -> when (appsSubPage) {
        AppsSubPage.LIST -> viewFactory.buildAppsListSection(
          onXhsSearch = {
            appsSubPage = AppsSubPage.XHS_SEARCH
            renderSection(stateStore.snapshot(), force = true)
          },
        )
        AppsSubPage.XHS_SEARCH -> viewFactory.buildXhsSearchSection(xhsConfigStore) { config: XhsSearchConfig ->
          if (config.keyword.isBlank()) {
            toast("请输入关键词")
          } else {
            toast("搜索: ${config.keyword} (flow 骨架待接入)")
          }
        }
      }
    }
    views.contentHost.addView(sectionView)
    renderedSection = currentSection
    renderedSectionKey = sectionKey
  }

  private fun updatePositions() {
    bubbleParams.x = clampBubbleX(bubbleParams.x)
    bubbleParams.y = clampBubbleY(bubbleParams.y)
    panelParams.x = panelXForBubble()
    panelParams.y = clampPanelY(bubbleParams.y - dp(24))
    if (::views.isInitialized) {
      windowManager.updateViewLayout(views.bubbleRoot, bubbleParams)
      windowManager.updateViewLayout(views.panelRoot, panelParams)
    }
  }

  private fun bubbleParams(): WindowManager.LayoutParams {
    return WindowManager.LayoutParams(
      bubbleSize(),
      bubbleSize(),
      overlayType(),
      WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
      PixelFormat.TRANSLUCENT,
    ).apply {
      gravity = Gravity.TOP or Gravity.START
      x = rightDockX()
      y = defaultBubbleY()
    }
  }

  private fun panelParams(): WindowManager.LayoutParams {
    return WindowManager.LayoutParams(
      panelWidth(),
      WindowManager.LayoutParams.WRAP_CONTENT,
      overlayType(),
      WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
      PixelFormat.TRANSLUCENT,
    ).apply {
      gravity = Gravity.TOP or Gravity.START
      softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING
      x = panelXForBubble()
      y = clampPanelY(defaultBubbleY() - dp(24))
    }
  }

  private fun dismissParams(): WindowManager.LayoutParams {
    return WindowManager.LayoutParams(
      WindowManager.LayoutParams.MATCH_PARENT,
      WindowManager.LayoutParams.MATCH_PARENT,
      overlayType(),
      WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
      PixelFormat.TRANSLUCENT,
    ).apply {
      gravity = Gravity.TOP or Gravity.START
      x = 0
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

  private fun screenHeight(): Int = resources.displayMetrics.heightPixels

  private fun bubbleSize(): Int = dp(72)

  private fun panelWidth(): Int = dp(248)

  private fun bubbleVisibleWidth(): Int = dp(38)

  private fun rightDockX(): Int = screenWidth() - bubbleVisibleWidth()

  private fun leftDockX(): Int = bubbleVisibleWidth() - bubbleSize()

  private fun defaultBubbleY(): Int = clampBubbleY(dp(160))

  private fun snapBubbleToEdge(x: Int): Int {
    return if (x + bubbleSize() / 2 < screenWidth() / 2) leftDockX() else rightDockX()
  }

  private fun clampBubbleX(x: Int): Int {
    return min(max(x, leftDockX()), rightDockX())
  }

  private fun clampBubbleY(y: Int): Int {
    val top = dp(96)
    val bottom = max(top, screenHeight() - bubbleSize() - dp(120))
    return min(max(y, top), bottom)
  }

  private fun clampPanelY(y: Int): Int {
    val top = dp(88)
    val bottom = max(top, screenHeight() - dp(280))
    return min(max(y, top), bottom)
  }

  private fun panelXForBubble(): Int {
    return if (bubbleParams.x <= leftDockX() / 2) {
      bubbleVisibleWidth() + dp(8)
    } else {
      max(dp(8), screenWidth() - panelWidth() - bubbleVisibleWidth() - dp(8))
    }
  }

  private fun dp(value: Int): Int = max(1, (value * resources.displayMetrics.density).toInt())

  private fun openConnectionConfig() {
    suspendOverlay()
    handler.post {
      applicationContext.startActivity(
        Intent(this, DaemonConfigActivity::class.java).apply {
          addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
          putExtra(DaemonConfigActivity.EXTRA_RESTORE_OVERLAY, true)
        },
      )
    }
  }

  private fun removeViewSafely(view: View) {
    runCatching { windowManager.removeView(view) }
  }

  private fun toast(message: String) {
    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
  }

  companion object {
    const val ACTION_SHOW = "com.flowy.explore.action.WORKBENCH_SHOW"
    const val ACTION_HIDE = "com.flowy.explore.action.WORKBENCH_HIDE"
    const val ACTION_SUSPEND = "com.flowy.explore.action.WORKBENCH_SUSPEND"

    @Volatile var isShowing: Boolean = false
  }

  enum class Section {
    AGENT_CONTROL,
    CAPTURE_MODE,
    SYSTEM_SETTINGS,
    APPS,
  }

  enum class AppsSubPage {
    LIST,
    XHS_SEARCH,
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
