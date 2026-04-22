package com.flowy.explore.foundation

import android.graphics.Bitmap
import java.io.ByteArrayOutputStream

object BitmapEncoder {
  fun toPng(bitmap: Bitmap): ByteArray {
    val output = ByteArrayOutputStream()
    bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
    return output.toByteArray()
  }
}
