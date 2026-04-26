package com.flowy.explore.flows

import com.flowy.explore.foundation.DisplayInfo
import com.flowy.explore.foundation.ObservedPageState
import com.flowy.explore.runtime.AccessibilitySnapshot
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkflowStepFlowTest {
  @Test
  fun run_executesObserveFilterAnchorOperationLoop() {
    val sent = mutableListOf<String>()
    val events = mutableListOf<String>()
    var observeCount = 0
    val flow = WorkflowStepFlow(
      logInfo = { _, _, _, _, _ -> Unit },
      logError = { _, _, _, _, _ -> Unit },
      versionName = { "0.1.0078" },
      sendResponse = {
        sent += it
        true
      },
      observePage = { _, _, _, _ ->
        observeCount += 1
        when (observeCount) {
          1 -> observedState("搜索")
          2 -> observedState("搜索")
          else -> observedState("结果页")
        }
      },
      filterTargets = { _, _ ->
        JSONObject("""{"status":"ok","output":{"selectedTargetRef":"target:auto:0","targets":[{"targetRef":"target:auto:0","bounds":{"left":10,"top":20,"right":110,"bottom":220}}]}}""")
      },
      evaluateAnchor = { pageState, input ->
        val text = input.optJSONArray("mustContainTexts")?.optString(0).orEmpty()
        JSONObject().apply {
          put("status", "ok")
          put("output", JSONObject().apply {
            put("matched", pageState.accessibilitySnapshot?.rawJson?.contains(text) == true)
            put("phase", input.getString("phase"))
          })
        }
      },
      executeOperation = { kind, payload, operationId ->
        JSONObject().apply {
          put("status", "ok")
          put("output", JSONObject().apply {
            put("operationId", operationId)
            put("message", "$kind:${payload.getInt("x")},${payload.getInt("y")}")
          })
        }
      },
      emitEvent = { eventType, _, _, _ ->
        events += eventType
        JSONObject("""{"status":"ok","output":{"accepted":true}}""")
      },
      sleepMs = {},
      nextDelayMs = { _, _ -> 0L },
      deviceInfo = { Triple("device-1", "model-1", "16") },
      now = { "2026-04-26T15:00:00+08:00" },
    )

    flow.run("req1", "run1", "run-workflow-step", JSONObject("""
      {
        "workflowId":"smoke",
        "observerSpec":{"requireAccessibility":true},
        "targetFilter":{"selector":{"textContains":"搜索"}},
        "operationPlan":{"kind":"tap","backend":"root"},
        "anchorPolicy":{
          "preAnchor":{"mustContainTexts":["搜索"]},
          "postAnchor":{"mustContainTexts":["结果页"]}
        },
        "postObservePolicy":{"maxAttempts":3,"pollIntervalMs":0}
      }
    """.trimIndent()))

    assertEquals(listOf("page.observed", "filter.matched", "anchor.pre.checked", "operation.finished", "page.observed", "anchor.post.checked", "workflow.step.succeeded"), events)
    assertTrue(sent.single().contains("\"status\":\"ok\""))
    assertTrue(sent.single().contains("workflow-step:tap:ok"))
  }

  @Test
  fun run_returnsErrorWhenPostAnchorDoesNotMatch() {
    val sent = mutableListOf<String>()
    val events = mutableListOf<String>()
    val flow = WorkflowStepFlow(
      logInfo = { _, _, _, _, _ -> Unit },
      logError = { _, _, _, _, _ -> Unit },
      versionName = { "0.1.0078" },
      sendResponse = {
        sent += it
        true
      },
      observePage = { _, _, _, _ -> observedState("搜索") },
      filterTargets = { _, _ ->
        JSONObject("""{"status":"ok","output":{"selectedTargetRef":"target:auto:0","targets":[{"targetRef":"target:auto:0","bounds":{"left":10,"top":20,"right":110,"bottom":220}}]}}""")
      },
      evaluateAnchor = { _, input ->
        val phase = input.getString("phase")
        JSONObject("""{"status":"ok","output":{"matched":${phase == "pre"},"phase":"$phase"}}""")
      },
      executeOperation = { _, _, _ -> JSONObject("""{"status":"ok","output":{"message":"tap"}}""") },
      emitEvent = { eventType, _, _, _ ->
        events += eventType
        JSONObject("""{"status":"ok","output":{"accepted":true}}""")
      },
      sleepMs = {},
      nextDelayMs = { _, _ -> 0L },
      deviceInfo = { Triple("device-1", "model-1", "16") },
      now = { "2026-04-26T15:00:00+08:00" },
    )

    flow.run("req1", "run1", "run-workflow-step", JSONObject("""
      {
        "observerSpec":{"requireAccessibility":true},
        "targetFilter":{"selector":{"textContains":"搜索"}},
        "operationPlan":{"kind":"tap","backend":"root"},
        "anchorPolicy":{
          "preAnchor":{"mustContainTexts":["搜索"]},
          "postAnchor":{"mustContainTexts":["结果页"]}
        },
        "postObservePolicy":{"maxAttempts":2,"pollIntervalMs":0}
      }
    """.trimIndent()))

    assertTrue(events.contains("workflow.step.failed"))
    assertTrue(sent.single().contains("\"status\":\"error\""))
    assertTrue(sent.single().contains("POST_ANCHOR_NOT_MATCHED"))
  }

  private fun observedState(text: String): ObservedPageState {
    return ObservedPageState(
      pageContext = JSONObject("""{"app":{"packageName":"com.flowy.explore"},"screen":{},"runtime":{"accessibilityReady":true}}"""),
      pageSignature = "sig-$text",
      displayInfo = DisplayInfo(1216, 2640, 560, 0, 0),
      accessibilitySnapshot = AccessibilitySnapshot(
        capturedAt = "2026-04-26T15:00:00+08:00",
        packageName = "com.flowy.explore",
        windowTitle = text,
        rawJson = """{"nodes":[{"text":"$text"}]}""",
      ),
      screenshotCapture = null,
    )
  }
}
