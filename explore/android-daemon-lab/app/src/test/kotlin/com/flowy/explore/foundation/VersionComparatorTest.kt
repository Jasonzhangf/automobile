package com.flowy.explore.foundation

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VersionComparatorTest {
  @Test
  fun detectsNewerBuild() {
    assertTrue(VersionComparator.isNewer("0.1.0035", "0.1.0034"))
  }

  @Test
  fun ignoresSameVersion() {
    assertFalse(VersionComparator.isNewer("0.1.0034", "0.1.0034"))
  }
}
