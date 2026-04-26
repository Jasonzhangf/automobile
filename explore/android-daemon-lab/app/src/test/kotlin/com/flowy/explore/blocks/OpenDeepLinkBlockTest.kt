package com.flowy.explore.blocks

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class OpenDeepLinkBlockTest {
  @Test
  fun run_supportsPackageLaunch() {
    var seenUri: String? = "unset"
    var seenPackage: String? = null
    var seenComponent: String? = "unset"
    val block = OpenDeepLinkBlock { uri, packageName, component ->
      seenUri = uri
      seenPackage = packageName
      seenComponent = component
    }

    val result = block.run(JSONObject().put("packageName", "com.xingin.xhs"))

    assertEquals("open-app:com.xingin.xhs", result)
    assertEquals("", seenUri)
    assertEquals("com.xingin.xhs", seenPackage)
    assertNull(seenComponent)
  }

  @Test
  fun run_supportsUriLaunch() {
    var seenUri: String? = null
    var seenPackage: String? = null
    var seenComponent: String? = null
    val block = OpenDeepLinkBlock { uri, packageName, component ->
      seenUri = uri
      seenPackage = packageName
      seenComponent = component
    }

    val result = block.run(JSONObject().put("uri", "xhsdiscover://home"))

    assertEquals("open-deep-link:xhsdiscover://home", result)
    assertEquals("xhsdiscover://home", seenUri)
    assertNull(seenPackage)
    assertNull(seenComponent)
  }

  @Test(expected = IllegalStateException::class)
  fun run_requiresUriPackageOrComponent() {
    OpenDeepLinkBlock { _, _, _ -> }.run(JSONObject())
  }

  @Test
  fun run_supportsExplicitComponent() {
    var seenUri: String? = null
    var seenPackage: String? = null
    var seenComponent: String? = null
    val block = OpenDeepLinkBlock { uri, packageName, component ->
      seenUri = uri
      seenPackage = packageName
      seenComponent = component
    }

    val result = block.run(JSONObject().put("component", "com.xingin.xhs/com.xingin.xhs.index.v2.IndexActivityV2"))

    assertEquals("open-component:com.xingin.xhs/com.xingin.xhs.index.v2.IndexActivityV2", result)
    assertEquals("", seenUri)
    assertNull(seenPackage)
    assertEquals("com.xingin.xhs/com.xingin.xhs.index.v2.IndexActivityV2", seenComponent)
  }
}
