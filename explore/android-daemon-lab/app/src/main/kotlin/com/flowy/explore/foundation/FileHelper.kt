package com.flowy.explore.foundation

import java.io.File

object FileHelper {
  fun ensureParent(file: File): File {
    file.parentFile?.mkdirs()
    return file
  }
}
