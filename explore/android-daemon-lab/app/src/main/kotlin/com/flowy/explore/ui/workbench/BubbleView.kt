package com.flowy.explore.ui.workbench

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.util.TypedValue
import android.view.Gravity
import android.widget.FrameLayout
import android.widget.ImageView

class BubbleView(context: Context) : FrameLayout(context) {
  private val iconView = ImageView(context).apply {
    adjustViewBounds = true
    scaleType = ImageView.ScaleType.FIT_CENTER
    setImageResource(com.flowy.explore.R.drawable.flowy_mark_light)
  }

  private val bubbleBackground = GradientDrawable().apply {
    shape = GradientDrawable.OVAL
    setColor(Color.parseColor("#D915171A"))
    setStroke(dp(4), Color.parseColor("#8A78909C"))
  }

  init {
    background = bubbleBackground
    alpha = 0.94f
    elevation = dp(8).toFloat()
    layoutParams = LayoutParams(dp(72), dp(72))
    addView(
      iconView,
      LayoutParams(dp(42), dp(42), Gravity.CENTER),
    )
    render("idle", Color.parseColor("#90A4AE"))
  }

  fun render(label: String, accentColor: Int) {
    bubbleBackground.setStroke(dp(4), accentColor)
    bubbleBackground.setColor(mixColor(accentColor))
    iconView.alpha = if (label == "stopped") 0.72f else 0.96f
  }

  private fun mixColor(accentColor: Int): Int {
    val r = (Color.red(accentColor) * 0.28f + 18).toInt().coerceAtMost(255)
    val g = (Color.green(accentColor) * 0.28f + 20).toInt().coerceAtMost(255)
    val b = (Color.blue(accentColor) * 0.28f + 24).toInt().coerceAtMost(255)
    return Color.argb(225, r, g, b)
  }

  private fun dp(value: Int): Int {
    return TypedValue.applyDimension(
      TypedValue.COMPLEX_UNIT_DIP,
      value.toFloat(),
      resources.displayMetrics,
    ).toInt()
  }
}
