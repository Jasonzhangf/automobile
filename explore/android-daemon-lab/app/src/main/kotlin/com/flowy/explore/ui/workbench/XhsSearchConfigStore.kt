package com.flowy.explore.ui.workbench

import android.content.Context

class XhsSearchConfigStore(context: Context) {
  private val prefs = context.getSharedPreferences("xhs_search_config", Context.MODE_PRIVATE)

  fun load(): XhsSearchConfig = XhsSearchConfig(
    keyword = prefs.getString("keyword", "") ?: "",
    maxPosts = prefs.getInt("maxPosts", 10),
    collectImages = prefs.getBoolean("collectImages", true),
    collectLinks = prefs.getBoolean("collectLinks", true),
    collectVideoLinks = prefs.getBoolean("collectVideoLinks", false),
    collectComments = prefs.getBoolean("collectComments", true),
    commentLimit = prefs.getInt("commentLimit", 0),
    likeTarget = runCatching { LikeTarget.valueOf(prefs.getString("likeTarget", "NONE") ?: "NONE") }.getOrElse { LikeTarget.NONE },
    likeKeyword = prefs.getString("likeKeyword", "") ?: "",
  )

  fun save(config: XhsSearchConfig) {
    prefs.edit()
      .putString("keyword", config.keyword)
      .putInt("maxPosts", config.maxPosts)
      .putBoolean("collectImages", config.collectImages)
      .putBoolean("collectLinks", config.collectLinks)
      .putBoolean("collectVideoLinks", config.collectVideoLinks)
      .putBoolean("collectComments", config.collectComments)
      .putInt("commentLimit", config.commentLimit)
      .putString("likeTarget", config.likeTarget.name)
      .putString("likeKeyword", config.likeKeyword)
      .apply()
  }
}
