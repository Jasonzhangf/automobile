package com.flowy.explore.foundation

object VersionComparator {
  fun isNewer(candidate: String, current: String): Boolean = compare(candidate, current) > 0

  fun compare(left: String, right: String): Int {
    val leftParts = left.split(".").map { it.toIntOrNull() ?: 0 }
    val rightParts = right.split(".").map { it.toIntOrNull() ?: 0 }
    val size = maxOf(leftParts.size, rightParts.size)
    repeat(size) { index ->
      val leftValue = leftParts.getOrElse(index) { 0 }
      val rightValue = rightParts.getOrElse(index) { 0 }
      if (leftValue != rightValue) return leftValue.compareTo(rightValue)
    }
    return 0
  }
}
