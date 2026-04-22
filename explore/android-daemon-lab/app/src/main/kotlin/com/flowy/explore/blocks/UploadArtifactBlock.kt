package com.flowy.explore.blocks

import com.flowy.explore.foundation.DevServerConfig
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

class UploadArtifactBlock(
  private val config: DevServerConfig,
  private val client: OkHttpClient = OkHttpClient(),
) {
  fun run(meta: JSONObject, content: ByteArray): JSONObject {
    val requestBody = MultipartBody.Builder()
      .setType(MultipartBody.FORM)
      .addFormDataPart("meta", meta.toString())
      .addFormDataPart(
        "file",
        meta.getString("fileName"),
        content.toRequestBody(meta.getString("contentType").toMediaType()),
      )
      .build()
    val request = Request.Builder()
      .url(config.artifactUrl())
      .post(requestBody)
      .build()
    client.newCall(request).execute().use { response ->
      if (!response.isSuccessful) {
        error("ARTIFACT_UPLOAD_FAILED")
      }
      val body = response.body?.string() ?: error("ARTIFACT_UPLOAD_FAILED")
      return JSONObject(body).getJSONObject("stored")
    }
  }
}
