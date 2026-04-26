package com.flowy.explore.ui.workbench

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.text.InputType
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Switch
import android.widget.TextView
import com.flowy.explore.foundation.LikeTarget
import com.flowy.explore.foundation.XhsSearchConfig

class XhsSearchFormFactory(private val context: Context) {
  fun build(
    config: XhsSearchConfig,
    onConfigChange: (XhsSearchConfig) -> Unit,
    onStart: (XhsSearchConfig) -> Unit,
  ): View {
    var currentConfig = config

    val keywordInput = EditText(context).apply {
      setText(config.keyword)
      setTextColor(Color.WHITE)
      setHintTextColor(Color.parseColor("#80B0BEC5"))
      hint = "搜索关键词"
      textSize = 11f
      inputType = InputType.TYPE_CLASS_TEXT
      background = roundedDrawable("#24313A44", "#5D74838F", 6, 1)
      setPadding(dp(6), dp(4), dp(6), dp(4))
      setSingleLine(true)
    }

    val maxPostsInput = EditText(context).apply {
      setText(config.maxPosts.toString())
      setTextColor(Color.WHITE)
      hint = "数量(1-50)"
      textSize = 11f
      inputType = InputType.TYPE_CLASS_NUMBER
      background = roundedDrawable("#24313A44", "#5D74838F", 6, 1)
      setPadding(dp(6), dp(4), dp(6), dp(4))
      setSingleLine(true)
    }

    val collectImagesSwitch = toggleSwitch("采集图片", config.collectImages)
    val collectLinksSwitch = toggleSwitch("采集链接", config.collectLinks)
    val collectVideoLinksSwitch = toggleSwitch("采集视频链接", config.collectVideoLinks)
    val collectCommentsSwitch = toggleSwitch("采集评论", config.collectComments)

    val commentLimitInput = EditText(context).apply {
      setText(if (config.commentLimit == 0) "" else config.commentLimit.toString())
      setTextColor(Color.WHITE)
      hint = "评论条数(0=不限)"
      textSize = 11f
      inputType = InputType.TYPE_CLASS_NUMBER
      background = roundedDrawable("#24313A44", "#5D74838F", 6, 1)
      setPadding(dp(6), dp(4), dp(6), dp(4))
      setSingleLine(true)
    }

    val likeGroup = RadioGroup(context).apply {
      orientation = RadioGroup.HORIZONTAL
      addView(likeRadio("不点赞", config.likeTarget == LikeTarget.NONE), weightedWrap(1f))
      addView(likeRadio("赞帖子", config.likeTarget == LikeTarget.POST), weightedWrap(1f))
      addView(likeRadio("赞评论", config.likeTarget == LikeTarget.KEYWORD_COMMENT), weightedWrap(1f))
    }

    val likeKeywordInput = EditText(context).apply {
      setText(config.likeKeyword)
      setTextColor(Color.WHITE)
      hint = "评论点赞关键词"
      textSize = 11f
      inputType = InputType.TYPE_CLASS_TEXT
      background = roundedDrawable("#24313A44", "#5D74838F", 6, 1)
      setPadding(dp(6), dp(4), dp(6), dp(4))
      setSingleLine(true)
      visibility = if (config.likeTarget == LikeTarget.KEYWORD_COMMENT) View.VISIBLE else View.GONE
    }

    likeGroup.setOnCheckedChangeListener { _, checkedId ->
      likeKeywordInput.visibility = if (checkedId == 2) View.VISIBLE else View.GONE
    }

    val startButton = Button(context).apply {
      text = "开始搜索"
      textSize = 12f
      setTextColor(Color.WHITE)
      setBackgroundColor(Color.parseColor("#37474F"))
      setOnClickListener {
        val likeTarget = when (likeGroup.checkedRadioButtonId) {
          1 -> LikeTarget.POST
          2 -> LikeTarget.KEYWORD_COMMENT
          else -> LikeTarget.NONE
        }
        val newConfig = XhsSearchConfig(
          keyword = keywordInput.text.toString().trim(),
          maxPosts = maxPostsInput.text.toString().toIntOrNull()?.coerceIn(1, 50) ?: 10,
          collectImages = collectImagesSwitch.isChecked,
          collectLinks = collectLinksSwitch.isChecked,
          collectVideoLinks = collectVideoLinksSwitch.isChecked,
          collectComments = collectCommentsSwitch.isChecked,
          commentLimit = commentLimitInput.text.toString().toIntOrNull() ?: 0,
          likeTarget = likeTarget,
          likeKeyword = likeKeywordInput.text.toString().trim(),
        )
        onConfigChange(newConfig)
        onStart(newConfig)
      }
    }

    return LinearLayout(context).apply {
      orientation = LinearLayout.VERTICAL
      setBackgroundColor(Color.parseColor("#22252A30"))
      setPadding(dp(6), dp(6), dp(6), dp(6))
      addView(labelText("小红书搜索"), matchWrap())
      addView(keywordInput, topMargin(matchWrap(), 4))
      addView(maxPostsInput, topMargin(matchWrap(), 4))
      addView(switchRow(collectImagesSwitch, collectLinksSwitch), topMargin(matchWrap(), 4))
      addView(collectVideoLinksSwitch, topMargin(matchWrap(), 2))
      addView(divider(), topMargin(verticalParams(), 4))
      addView(labelText("评论"), topMargin(matchWrap(), 2))
      addView(switchRow(collectCommentsSwitch, commentLimitInput), topMargin(matchWrap(), 2))
      addView(divider(), topMargin(verticalParams(), 4))
      addView(labelText("点赞"), topMargin(matchWrap(), 2))
      addView(likeGroup, topMargin(matchWrap(), 2))
      addView(likeKeywordInput, topMargin(matchWrap(), 2))
      addView(startButton, topMargin(matchWrap(), 6))
    }
  }

  private fun toggleSwitch(label: String, checked: Boolean): Switch {
    return Switch(context).apply {
      text = label
      isChecked = checked
      setTextColor(Color.parseColor("#CFD8DC"))
      textSize = 11f
      setPadding(0, 0, 0, 0)
      minHeight = dp(0)
      thumbTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#78909C"))
      trackTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#37474F"))
    }
  }

  private fun switchRow(left: View, right: View): LinearLayout {
    return LinearLayout(context).apply {
      orientation = LinearLayout.HORIZONTAL
      gravity = Gravity.CENTER_VERTICAL
      addView(left, weightedWrap(1f))
      if (right is EditText) addView(right, LinearLayout.LayoutParams(dp(100), LinearLayout.LayoutParams.WRAP_CONTENT))
      else addView(right, weightedWrap(1f))
    }
  }

  private fun likeRadio(label: String, checked: Boolean): RadioButton {
    return RadioButton(context).apply {
      id = when (label) {
        "赞帖子" -> 1
        "赞评论" -> 2
        else -> 0
      }
      text = label
      isChecked = checked
      setTextColor(Color.WHITE)
      textSize = 11f
      gravity = Gravity.CENTER
      buttonDrawable = null
      minHeight = dp(28)
      background = roundedDrawable(
        if (checked) "#5B78909C" else "#00000000",
        if (checked) "#B0C0CA" else "#00000000",
        10, if (checked) 1 else 0,
      )
    }
  }

  private fun labelText(text: String) = TextView(context).apply {
    this.text = text
    setTextColor(Color.WHITE)
    setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
  }

  private fun divider() = View(context).apply {
    setBackgroundColor(Color.parseColor("#22FFFFFF"))
    minimumHeight = dp(1)
  }

  private fun matchWrap() = LinearLayout.LayoutParams(
    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT,
  )
  private fun verticalParams() = LinearLayout.LayoutParams(
    LinearLayout.LayoutParams.MATCH_PARENT, dp(1),
  )
  private fun weightedWrap(w: Float) = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, w)
  private fun topMargin(p: LinearLayout.LayoutParams, top: Int): LinearLayout.LayoutParams {
    p.topMargin = dp(top); return p
  }
  private fun roundedDrawable(fill: String, stroke: String, r: Int, s: Int) = GradientDrawable().apply {
    shape = GradientDrawable.RECTANGLE; cornerRadius = dp(r).toFloat()
    setColor(Color.parseColor(fill)); if (s > 0) setStroke(dp(s), Color.parseColor(stroke))
  }
  private fun dp(v: Int) = TypedValue.applyDimension(
    TypedValue.COMPLEX_UNIT_DIP, v.toFloat(), context.resources.displayMetrics,
  ).toInt()
}
