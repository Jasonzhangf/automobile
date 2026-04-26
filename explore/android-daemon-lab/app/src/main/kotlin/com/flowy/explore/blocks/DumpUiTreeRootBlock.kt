package com.flowy.explore.blocks

import com.flowy.explore.foundation.RootShellRunner
import com.flowy.explore.foundation.TimeHelper
import com.flowy.explore.runtime.AccessibilitySnapshot
import org.json.JSONArray
import org.json.JSONObject
import java.io.StringReader
import javax.xml.parsers.DocumentBuilderFactory
import org.xml.sax.InputSource

/**
 * 用 root 权限执行 `uiautomator dump` 获取当前页面 UI 节点树，
 * 解析 XML 产出和 AccessibilitySnapshot 兼容的 JSON 结构，
 * 使得 FilterTargetsBlock / EvaluateAnchorBlock 可以无缝消费。
 */
class DumpUiTreeRootBlock(
  private val runRoot: (String) -> RootShellRunner.Result = { RootShellRunner().run(it) },
  private val now: () -> String = TimeHelper::now,
) {
  fun run(): AccessibilitySnapshot {
    val tmpPath = "/data/local/tmp/flowy-ui-dump.xml"
    val result = runRoot("uiautomator dump $tmpPath && cat $tmpPath")
    check(result.exitCode == 0) { "ROOT_UI_DUMP_FAILED" }
    val raw = result.stdoutText()
    val xmlContent = extractXml(raw)
    val snapshot = parseXmlToSnapshot(xmlContent)
    return AccessibilitySnapshot(
      capturedAt = now(),
      packageName = snapshot.optString("packageName").ifBlank { null },
      windowTitle = null,
      rawJson = snapshot.toString(),
    )
  }

  private fun extractXml(raw: String): String {
    val start = raw.indexOf("<?xml")
    if (start >= 0) return raw.substring(start)
    val altStart = raw.indexOf("<hierarchy")
    if (altStart >= 0) return raw.substring(altStart)
    error("ROOT_UI_DUMP_NO_XML")
  }

  private fun parseXmlToSnapshot(xml: String): JSONObject {
    val factory = DocumentBuilderFactory.newInstance()
    factory.isNamespaceAware = false
    val builder = factory.newDocumentBuilder()
    val doc = builder.parse(InputSource(StringReader(xml)))
    val root = doc.documentElement
    val nodes = JSONArray()
    var packageName: String? = null
    fun walk(node: org.w3c.dom.Node) {
      if (node.nodeType != org.w3c.dom.Node.ELEMENT_NODE) return
      val el = node as org.w3c.dom.Element
      val pkg = el.getAttribute("package")
      if (pkg.isNotBlank() && packageName == null) packageName = pkg
      val boundsStr = el.getAttribute("bounds")
      val bounds = parseBounds(boundsStr)
      val obj = JSONObject().apply {
        put("text", el.getAttribute("text"))
        put("contentDescription", el.getAttribute("content-desc"))
        put("hintText", "")
        put("className", el.getAttribute("class"))
        put("packageName", pkg)
        put("resourceId", el.getAttribute("resource-id"))
        put("flags", JSONObject().apply {
          put("clickable", el.getAttribute("clickable") == "true")
          put("scrollable", el.getAttribute("scrollable") == "true")
          put("editable", el.getAttribute("class")?.contains("EditText") == true)
          put("checkable", el.getAttribute("checkable") == "true")
          put("checked", el.getAttribute("checked") == "true")
          put("enabled", el.getAttribute("enabled") == "true")
          put("focusable", el.getAttribute("focusable") == "true")
          put("focused", el.getAttribute("focused") == "true")
          put("longClickable", el.getAttribute("long-clickable") == "true")
          put("selected", el.getAttribute("selected") == "true")
          put("password", el.getAttribute("password") == "true")
        })
        if (bounds != null) {
          put("boundsInScreen", JSONObject().apply {
            put("left", bounds[0])
            put("top", bounds[1])
            put("right", bounds[2])
            put("bottom", bounds[3])
          })
        }
      }
      nodes.put(obj)
      val children = node.childNodes
      for (i in 0 until children.length) {
        walk(children.item(i))
      }
    }
    walk(root)
    return JSONObject().apply {
      put("packageName", packageName ?: "")
      put("nodes", nodes)
    }
  }

  private fun parseBounds(str: String): IntArray? {
    val match = Regex("\\[(\\d+),(\\d+)\\]\\[(\\d+),(\\d+)\\]").find(str) ?: return null
    return intArrayOf(
      match.groupValues[1].toInt(),
      match.groupValues[2].toInt(),
      match.groupValues[3].toInt(),
      match.groupValues[4].toInt(),
    )
  }
}
