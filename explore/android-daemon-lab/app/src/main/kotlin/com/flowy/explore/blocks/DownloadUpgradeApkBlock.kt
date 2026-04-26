package com.flowy.explore.blocks

import android.content.Context
import java.io.File
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

class DownloadUpgradeApkBlock(
  private val context: Context,
  private val client: OkHttpClient = OkHttpClient(),
) {
  fun run(manifestUrl: String): File {
    val manifestRequest = Request.Builder().url(manifestUrl).get().build()
    val manifest = client.newCall(manifestRequest).execute().use { response ->
      if (!response.isSuccessful) error("upgrade manifest failed: ${response.code}")
      JSONObject(response.body?.string() ?: error("missing manifest body"))
    }
    val fileName = manifest.optString("fileName", "flowy-upgrade.apk")
    val downloadUrl = manifest.optString("downloadUrl")
    val apkRequest = Request.Builder().url(downloadUrl).get().build()
    val output = File(context.cacheDir, fileName)
    client.newCall(apkRequest).execute().use { response ->
      if (!response.isSuccessful) error("apk download failed: ${response.code}")
      output.outputStream().use { sink ->
        sink.write(response.body?.bytes() ?: error("missing apk body"))
      }
    }
    return output
  }
}
