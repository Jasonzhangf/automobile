package com.flowy.explore.foundation

import org.junit.Assert.assertTrue
import org.junit.Test

class TimeHelperTest {
  @Test
  fun nowReturnsIsoLikeValue() {
    assertTrue(TimeHelper.now().contains("T"))
  }
}
