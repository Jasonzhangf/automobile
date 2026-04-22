package com.flowy.explore.ui.workbench

import android.content.Context
import android.graphics.Color
import android.text.InputType
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.ScrollView
import android.widget.TextView

class WorkbenchViewFactory(private val context: Context) {
  data class Views(
    val bubbleRoot: TextView,
    val panelRoot: ScrollView,
    val statusTitle: TextView,
    val statusBody: TextView,
    val agentControlButton: Button,
    val captureModeButton: Button,
    val systemSettingsButton: Button,
    val contentHost: LinearLayout,
  )

  fun createViews(): Views {
    val bubbleRoot = TextView(context).apply {
      setBackgroundColor(Color.parseColor("#CC37474F"))
      setTextColor(Color.WHITE)
      gravity = Gravity.CENTER
      textSize = 12f
      text = "idle"
      setPadding(dp(12), dp(12), dp(12), dp(12))
      minWidth = dp(56)
      minHeight = dp(56)
      alpha = 0.88f
    }

    val statusTitle = headerText("workbench")
    val statusBody = bodyText("state loading")
    val agentControlButton = menuButton("Agent 自身控制")
    val captureModeButton = menuButton("捕获模式")
    val systemSettingsButton = menuButton("系统设置")
    val contentHost = LinearLayout(context).apply {
      orientation = LinearLayout.VERTICAL
    }
    val menuRow = LinearLayout(context).apply {
      orientation = LinearLayout.VERTICAL
      addView(agentControlButton, matchWrap())
      addView(captureModeButton, topMargin(matchWrap(), 8))
      addView(systemSettingsButton, topMargin(matchWrap(), 8))
    }
    val container = LinearLayout(context).apply {
      orientation = LinearLayout.VERTICAL
      setBackgroundColor(Color.parseColor("#E6222222"))
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
      panelRoot,
      statusTitle,
      statusBody,
      agentControlButton,
      captureModeButton,
      systemSettingsButton,
      contentHost,
    )
  }

  fun buildAgentControlSection(
    snapshot: WorkbenchStatusSnapshot,
    onPassiveMode: () -> Unit,
    onActiveMode: () -> Unit,
    onHostCommit: (String) -> Unit,
    onPortCommit: (String) -> Unit,
  ): View {
    val modeGroup = RadioGroup(context).apply {
      orientation = RadioGroup.HORIZONTAL
      addView(radio("被控模式", snapshot.agentMode.name == "PASSIVE") { onPassiveMode() })
      addView(radio("主控模式", snapshot.agentMode.name == "ACTIVE") { onActiveMode() })
    }
    val hostInput = input("daemon ip", snapshot.host, InputType.TYPE_CLASS_TEXT) { onHostCommit(it) }
    val portInput = input("port", snapshot.port.toString(), InputType.TYPE_CLASS_NUMBER) { onPortCommit(it) }
    return section("Agent 自身控制", listOf(
      bodyText("连接目标会从这里即时更新；若 daemon 已运行，建议重新启动连接。"),
      modeGroup,
      hostInput,
      portInput,
    ))
  }

  fun buildCaptureSection(snapshot: WorkbenchStatusSnapshot, onEnter: () -> Unit, onExit: () -> Unit): View {
    val enter = actionButton("进入捕获模式") { onEnter() }
    val exit = actionButton("退出捕获模式") { onExit() }
    val body = bodyText(
      if (snapshot.captureModeEnabled) {
        "捕获模式已激活：下一步接入长按选中 / alias / test / save。"
      } else {
        "捕获模式未激活：当前先落 UI 骨架，后续接入真实 target 选中闭环。"
      },
    )
    return section("捕获模式", listOf(body, enter, exit))
  }

  fun buildSystemSection(
    snapshot: WorkbenchStatusSnapshot,
    onOpenOverlayPermission: () -> Unit,
    onOpenAccessibility: () -> Unit,
    onOpenDevPanel: () -> Unit,
    onCheckUpgrade: () -> Unit,
  ): View {
    return section("系统设置", listOf(
      bodyText("overlay=${snapshot.overlayPermissionStatus}, accessibility=${snapshot.accessibilityStatus}"),
      actionButton("悬浮窗权限") { onOpenOverlayPermission() },
      actionButton("无障碍权限") { onOpenAccessibility() },
      actionButton("打开 Dev Panel") { onOpenDevPanel() },
      actionButton("检查升级") { onCheckUpgrade() },
    ))
  }

  private fun section(title: String, views: List<View>): View {
    return LinearLayout(context).apply {
      orientation = LinearLayout.VERTICAL
      setBackgroundColor(Color.parseColor("#1A90A4AE"))
      setPadding(dp(12), dp(12), dp(12), dp(12))
      addView(headerText(title), matchWrap())
      views.forEachIndexed { index, view ->
        addView(view, if (index == 0) topMargin(matchWrap(), 8) else topMargin(matchWrap(), 10))
      }
    }
  }

  private fun radio(text: String, checked: Boolean, onClick: () -> Unit): RadioButton {
    return RadioButton(context).apply {
      this.text = text
      isChecked = checked
      setTextColor(Color.WHITE)
      setOnClickListener { onClick() }
    }
  }

  private fun input(hint: String, value: String, inputType: Int, onCommit: (String) -> Unit): EditText {
    return EditText(context).apply {
      this.hint = hint
      setText(value)
      this.inputType = inputType
      setHintTextColor(Color.parseColor("#90A4AE"))
      setTextColor(Color.WHITE)
      setBackgroundColor(Color.parseColor("#33222222"))
      setPadding(dp(10), dp(10), dp(10), dp(10))
      setOnFocusChangeListener { _, hasFocus -> if (!hasFocus) onCommit(text.toString()) }
    }
  }

  private fun actionButton(text: String, onClick: () -> Unit): Button = Button(context).apply {
    this.text = text
    setOnClickListener { onClick() }
  }

  private fun menuButton(text: String): Button = Button(context).apply {
    this.text = text
    setBackgroundColor(Color.parseColor("#455A64"))
    setTextColor(Color.WHITE)
  }

  private fun headerText(text: String): TextView = TextView(context).apply {
    this.text = text
    setTextColor(Color.WHITE)
    setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
  }

  private fun bodyText(text: String): TextView = TextView(context).apply {
    this.text = text
    setTextColor(Color.parseColor("#CFD8DC"))
    setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
  }

  private fun matchWrap() = LinearLayout.LayoutParams(
    LinearLayout.LayoutParams.MATCH_PARENT,
    LinearLayout.LayoutParams.WRAP_CONTENT,
  )

  private fun topMargin(params: LinearLayout.LayoutParams, top: Int): LinearLayout.LayoutParams {
    params.topMargin = dp(top)
    return params
  }

  private fun dp(value: Int): Int {
    return TypedValue.applyDimension(
      TypedValue.COMPLEX_UNIT_DIP,
      value.toFloat(),
      context.resources.displayMetrics,
    ).toInt()
  }
}
