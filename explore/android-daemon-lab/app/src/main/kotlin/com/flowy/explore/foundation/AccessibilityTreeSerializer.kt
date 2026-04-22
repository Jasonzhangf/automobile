package com.flowy.explore.foundation

import android.graphics.Rect
import android.view.accessibility.AccessibilityNodeInfo
import com.flowy.explore.runtime.AccessibilitySnapshot
import org.json.JSONArray
import org.json.JSONObject

class AccessibilityTreeSerializer {
  fun serialize(root: AccessibilityNodeInfo): AccessibilitySnapshot {
    var nextId = 0
    val nodes = mutableListOf<JSONObject>()
    val rootNodeId = "n${nextId++}"
    walk(root, rootNodeId, null, nodes) { nextId++ }
    val capturedAt = TimeHelper.now()
    return AccessibilitySnapshot(
      capturedAt = capturedAt,
      packageName = root.packageName?.toString(),
      windowTitle = root.paneTitle?.toString(),
      rawJson = JSONObject().apply {
        put("capturedAt", capturedAt)
        put("packageName", root.packageName?.toString())
        put("rootNodeId", rootNodeId)
        put("nodeCount", nodes.size)
        put("nodes", JSONArray(nodes))
      }.toString(),
    )
  }

  private fun walk(
    node: AccessibilityNodeInfo,
    nodeId: String,
    parentId: String?,
    nodes: MutableList<JSONObject>,
    nextId: () -> Int,
  ) {
    val childIds = mutableListOf<String>()
    val children = mutableListOf<Pair<String, AccessibilityNodeInfo>>()
    for (index in 0 until node.childCount) {
      val child = node.getChild(index) ?: continue
      val childId = "n${nextId()}"
      childIds += childId
      children += childId to child
    }
    nodes += JSONObject().apply {
      put("nodeId", nodeId)
      put("parentId", parentId ?: JSONObject.NULL)
      put("childIds", JSONArray(childIds))
      put("className", node.className?.toString())
      put("packageName", node.packageName?.toString())
      put("viewIdResourceName", node.viewIdResourceName ?: JSONObject.NULL)
      put("text", node.text?.toString() ?: JSONObject.NULL)
      put("contentDescription", node.contentDescription?.toString() ?: JSONObject.NULL)
      put("hintText", node.hintText?.toString() ?: JSONObject.NULL)
      put("paneTitle", node.paneTitle?.toString() ?: JSONObject.NULL)
      put("boundsInScreen", node.boundsJson())
      put("flags", JSONObject().apply {
        put("enabled", node.isEnabled)
        put("clickable", node.isClickable)
        put("longClickable", node.isLongClickable)
        put("focusable", node.isFocusable)
        put("focused", node.isFocused)
        put("checkable", node.isCheckable)
        put("checked", node.isChecked)
        put("selected", node.isSelected)
        put("scrollable", node.isScrollable)
        put("editable", node.isEditable)
        put("visibleToUser", node.isVisibleToUser)
        put("password", node.isPassword)
      })
    }
    children.forEach { (childId, child) ->
      child.use {
        walk(it, childId, nodeId, nodes, nextId)
      }
    }
  }

  private fun AccessibilityNodeInfo.boundsJson(): JSONObject {
    val rect = Rect()
    getBoundsInScreen(rect)
    return JSONObject().apply {
      put("left", rect.left)
      put("top", rect.top)
      put("right", rect.right)
      put("bottom", rect.bottom)
    }
  }

  private inline fun AccessibilityNodeInfo.use(block: (AccessibilityNodeInfo) -> Unit) {
    try {
      block(this)
    } finally {
      recycle()
    }
  }
}
