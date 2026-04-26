package com.flowy.explore.ui.workbench

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.ScrollView
import android.widget.TextView
import com.flowy.explore.foundation.XhsSearchConfig
import com.flowy.explore.foundation.XhsSearchConfigStore

class WorkbenchViewFactory(private val context: Context) {
  data class Views(
    val bubbleRoot: BubbleView,
    val dismissRoot: View,
    val panelRoot: ScrollView,
    val statusTitle: TextView,
    val statusBody: TextView,
    val agentControlButton: Button,
    val captureModeButton: Button,
    val systemSettingsButton: Button,
    val upgradeButton: Button,
    val manualUpgradeButton: Button,
    val appsButton: Button,
    val contentHost: LinearLayout,
  )

  fun createViews(): Views {
    val bubbleRoot = BubbleView(context)
    val dismissRoot = View(context).apply {
      setBackgroundColor(Color.TRANSPARENT)
      alpha = 0.01f
    }

    val statusTitle = headerText("workbench")
    val statusBody = bodyText("state loading")
    val agentControlButton = menuButton("Agent 自身控制")
    val captureModeButton = menuButton("捕获模式")
    val systemSettingsButton = menuButton("系统设置")
    val upgradeButton = menuButton("检查升级")
    val manualUpgradeButton = menuButton("手动升级")
    val appsButton = menuButton("应用")
    val contentHost = LinearLayout(context).apply {
      orientation = LinearLayout.VERTICAL
    }
    val menuRow = LinearLayout(context).apply {
      orientation = LinearLayout.VERTICAL
      addView(agentControlButton, matchWrap())
      addView(captureModeButton, topMargin(matchWrap(), 8))
      addView(systemSettingsButton, topMargin(matchWrap(), 8))
      addView(upgradeButton, topMargin(matchWrap(), 8))
      addView(manualUpgradeButton, topMargin(matchWrap(), 8))
      addView(appsButton, topMargin(matchWrap(), 8))
    }
    val container = LinearLayout(context).apply {
      orientation = LinearLayout.VERTICAL
      setBackgroundColor(Color.parseColor("#EA15171A"))
      setPadding(dp(14), dp(14), dp(14), dp(14))
      addView(statusTitle, matchWrap())
      addView(statusBody, topMargin(matchWrap(), 8))
      addView(menuRow, topMargin(matchWrap(), 16))
      addView(contentHost, topMargin(matchWrap(), 16))
    }
    val panelRoot = ScrollView(context).apply {
      isFillViewport = true
      alpha = 0.96f
      addView(container, matchWrap())
    }
    return Views(
      bubbleRoot,
      dismissRoot,
      panelRoot,
      statusTitle,
      statusBody,
      agentControlButton,
      captureModeButton,
      systemSettingsButton,
      upgradeButton,
      manualUpgradeButton,
      appsButton,
      contentHost,
    )
  }

  fun buildAgentControlSection(
    snapshot: WorkbenchStatusSnapshot,
    onPassiveMode: () -> Unit,
    onActiveMode: () -> Unit,
    onOpenConnectionConfig: () -> Unit,
  ): View {
    val modeGroup = RadioGroup(context).apply {
      orientation = RadioGroup.HORIZONTAL
      setPadding(dp(4), dp(4), dp(4), dp(4))
      background = roundedDrawable("#24313A44", "#5D74838F", 16, 1)
      addView(radio("被控模式", snapshot.agentMode.name == "PASSIVE") { onPassiveMode() }, weightedWrap(1f))
      addView(radio("主控模式", snapshot.agentMode.name == "ACTIVE") { onActiveMode() }, weightedWrap(1f))
    }
    val endpointSummary = bodyText("连接目标 ${snapshot.host}:${snapshot.port}")
    val endpointButton = actionButton("连接配置") { onOpenConnectionConfig() }
    return section("Agent 自身控制", listOf(
      bodyText("主菜单只保留模式与状态；输入移到次级配置页。"),
      modeGroup,
      endpointSummary,
      endpointButton,
    ))
  }

  fun buildCaptureSection(snapshot: WorkbenchStatusSnapshot, onEnter: () -> Unit, onExit: () -> Unit): View {
    val enter = actionButton("进入捕获模式") { onEnter() }
    val exit = actionButton("退出捕获模式") { onExit() }
    val body = bodyText(
      if (snapshot.captureModeEnabled) {
        "当前已开启，可继续做目标标记。"
      } else {
        "当前未开启。"
      },
    )
    return section("捕获模式", listOf(body, enter, exit))
  }

  fun buildSystemSection(
    snapshot: WorkbenchStatusSnapshot,
    onOpenOverlayPermission: () -> Unit,
    onOpenAccessibility: () -> Unit,
    onOpenDevPanel: () -> Unit,
  ): View {
    return section("系统设置", listOf(
      bodyText("悬浮=${snapshot.overlayPermissionStatus} · 无障碍=${snapshot.accessibilityStatus}"),
      actionButton("悬浮窗权限") { onOpenOverlayPermission() },
      actionButton("无障碍权限") { onOpenAccessibility() },
      actionButton("打开 Dev Panel") { onOpenDevPanel() },
    ))
  }

  fun buildAppsListSection(onXhsSearch: () -> Unit): View {
    val xhsButton = actionButton("小红书搜索") { onXhsSearch() }
    return section("应用", listOf(
      bodyText("选择要运行的自动化流程"),
      xhsButton,
    ))
  }

  fun buildXhsSearchSection(configStore: XhsSearchConfigStore, onStart: (XhsSearchConfig) -> Unit): View {
    val config = configStore.load()
    val formFactory = XhsSearchFormFactory(context)
    val formView = formFactory.build(
      config = config,
      onConfigChange = { configStore.save(it) },
      onStart = onStart,
    )
    return section("小红书搜索", listOf(formView))
  }

  private fun section(title: String, views: List<View>): View {
    return LinearLayout(context).apply {
      orientation = LinearLayout.VERTICAL
      setBackgroundColor(Color.parseColor("#22252A30"))
      setPadding(dp(10), dp(10), dp(10), dp(10))
      addView(headerText(title), matchWrap())
      views.forEachIndexed { index, view ->
        addView(view, if (index == 0) topMargin(matchWrap(), 6) else topMargin(matchWrap(), 8))
      }
    }
  }

  private fun radio(text: String, checked: Boolean, onClick: () -> Unit): RadioButton {
    return RadioButton(context).apply {
      this.text = text
      isChecked = checked
      setTextColor(Color.WHITE)
      gravity = Gravity.CENTER
      buttonDrawable = null
      minHeight = dp(40)
      background = roundedDrawable(
        if (checked) "#5B78909C" else "#00000000",
        if (checked) "#B0C0CA" else "#00000000",
        12,
        if (checked) 1 else 0,
      )
      setOnClickListener { onClick() }
    }
  }

  private fun actionButton(text: String, onClick: () -> Unit): Button = Button(context).apply {
    this.text = text
    textSize = 14f
    minHeight = dp(40)
    setBackgroundColor(Color.parseColor("#455A64"))
    setTextColor(Color.WHITE)
    setOnClickListener { onClick() }
  }

  private fun menuButton(text: String): Button = Button(context).apply {
    this.text = text
    textSize = 14f
    minHeight = dp(40)
    setBackgroundColor(Color.parseColor("#37474F"))
    setTextColor(Color.WHITE)
  }

  private fun headerText(text: String): TextView = TextView(context).apply {
    this.text = text
    setTextColor(Color.WHITE)
    setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
  }

  private fun bodyText(text: String): TextView = TextView(context).apply {
    this.text = text
    setTextColor(Color.parseColor("#CFD8DC"))
    setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
  }

  private fun matchWrap() = LinearLayout.LayoutParams(
    LinearLayout.LayoutParams.MATCH_PARENT,
    LinearLayout.LayoutParams.WRAP_CONTENT,
  )

  private fun weightedWrap(weight: Float) = LinearLayout.LayoutParams(
    0,
    LinearLayout.LayoutParams.WRAP_CONTENT,
    weight,
  )

  private fun topMargin(params: LinearLayout.LayoutParams, top: Int): LinearLayout.LayoutParams {
    params.topMargin = dp(top)
    return params
  }

  private fun roundedDrawable(fill: String, stroke: String, radiusDp: Int, strokeDp: Int): GradientDrawable {
    return GradientDrawable().apply {
      shape = GradientDrawable.RECTANGLE
      cornerRadius = dp(radiusDp).toFloat()
      setColor(Color.parseColor(fill))
      if (strokeDp > 0) setStroke(dp(strokeDp), Color.parseColor(stroke))
    }
  }

  private fun dp(value: Int): Int {
    return TypedValue.applyDimension(
      TypedValue.COMPLEX_UNIT_DIP,
      value.toFloat(),
      context.resources.displayMetrics,
    ).toInt()
  }
}
