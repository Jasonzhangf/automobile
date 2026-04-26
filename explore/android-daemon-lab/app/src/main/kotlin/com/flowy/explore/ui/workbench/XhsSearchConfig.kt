package com.flowy.explore.ui.workbench

enum class LikeTarget { NONE, POST, KEYWORD_COMMENT }

data class XhsSearchConfig(
  val keyword: String = "",
  val maxPosts: Int = 10,
  val collectImages: Boolean = true,
  val collectLinks: Boolean = true,
  val collectVideoLinks: Boolean = false,
  val collectComments: Boolean = true,
  val commentLimit: Int = 0,
  val likeTarget: LikeTarget = LikeTarget.NONE,
  val likeKeyword: String = "",
)
