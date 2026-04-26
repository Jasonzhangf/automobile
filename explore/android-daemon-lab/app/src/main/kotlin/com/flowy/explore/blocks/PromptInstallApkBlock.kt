package com.flowy.explore.blocks

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import java.io.File

class PromptInstallApkBlock(private val context: Context) {
  fun run(apkFile: File): String {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !context.packageManager.canRequestPackageInstalls()) {
      context.startActivity(
        Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
          data = Uri.parse("package:${context.packageName}")
          addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        },
      )
      return "install-permission-required"
    }
    val contentUri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", apkFile)
    context.startActivity(
      Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(contentUri, "application/vnd.android.package-archive")
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
      },
    )
    return "installer-opened"
  }
}
