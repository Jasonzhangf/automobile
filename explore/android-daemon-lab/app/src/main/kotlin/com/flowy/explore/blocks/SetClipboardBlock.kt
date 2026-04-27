package com.flowy.explore.blocks

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import com.flowy.explore.foundation.RootShellRunner

class SetClipboardBlock(
  private val context: Context,
) {
  fun run(text: String): Boolean {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText("flowy", text)
    clipboard.setPrimaryClip(clip)
    return true
  }
}
