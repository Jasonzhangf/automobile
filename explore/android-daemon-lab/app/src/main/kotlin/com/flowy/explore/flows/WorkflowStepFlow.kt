package com.flowy.explore.flows

import android.os.Build
import com.flowy.explore.foundation.AccessibilityTargeting
import com.flowy.explore.foundation.ObservedPageState
import com.flowy.explore.foundation.TimeHelper
import org.json.JSONArray
import org.json.JSONObject
import kotlin.random.Random

class WorkflowStepFlow(
  private val logInfo: (String, String, String, String, String) -> Unit,
  private val logError: (String, String, String, String, String) -> Unit,
  private val versionName: () -> String,
  private val sendResponse: (String) -> Boolean,
  private val observePage: (String, String, String, JSONObject) -> ObservedPageState,
  private val filterTargets: (ObservedPageState, JSONObject) -> JSONObject,
  private val evaluateAnchor: (ObservedPageState, JSONObject) -> JSONObject,
  private val executeOperation: (String, JSONObject, String) -> JSONObject,
  private val emitEvent: (String, String, String, JSONObject) -> JSONObject,
  private val sleepMs: (Long) -> Unit = Thread::sleep,
  private val nextDelayMs: (Long, Long) -> Long = { min, max -> Random.nextLong(min, max + 1) },
  private val deviceInfo: () -> Triple<String, String, String> = { Triple(Build.DEVICE, Build.MODEL, Build.VERSION.RELEASE) },
  private val now: () -> String = TimeHelper::now,
) {
  fun run(requestId: String, runId: String, command: String, payload: JSONObject) {
    val startedAt = now()
    logInfo("workflow_step_started", "starting $command", requestId, runId, command)
    try {
      val observerSpec = payload.optJSONObject("observerSpec") ?: JSONObject()
      val targetFilter = payload.optJSONObject("targetFilter") ?: error("TARGET_FILTER_MISSING")
      val operationPlan = payload.optJSONObject("operationPlan") ?: payload.optJSONObject("operation") ?: error("OPERATION_PLAN_MISSING")
      val operationKind = operationPlan.optString("kind").ifBlank { error("OPERATION_KIND_MISSING") }
      val operationId = operationPlan.optString("operationId").ifBlank { operationKind }
      val anchorPolicy = payload.optJSONObject("anchorPolicy") ?: JSONObject()
      val preObserved = observePage(requestId, runId, "observe-page", observerSpec)
      emitWorkflowEvent("page.observed", requestId, runId, preObserved.pageContext)
      val filtered = requireOk(filterTargets(preObserved, targetFilter), "FILTER_TARGETS_FAILED")
      val selectedTarget = selectedTarget(filtered)
      emitWorkflowEvent("filter.matched", requestId, runId, JSONObject().apply {
        put("selectedTargetRef", selectedTarget.optString("targetRef"))
        put("bounds", selectedTarget.optJSONObject("bounds") ?: JSONObject.NULL)
      })
      val preAnchor = anchorPolicy.optJSONObject("preAnchor")
      if (preAnchor != null) {
        val evaluated = requireMatched(preObserved, preAnchor, "pre")
        emitWorkflowEvent("anchor.pre.checked", requestId, runId, evaluated)
      }
      val operationPayload = buildOperationPayload(operationPlan, selectedTarget)
      val operationResult = requireOk(executeOperation(operationKind, operationPayload, operationId), "EXECUTE_OPERATION_FAILED")
      emitWorkflowEvent("operation.finished", requestId, runId, JSONObject().apply {
        put("operationId", operationId)
        put("message", operationResult.optString("message", operationKind))
      })
      sleepIfNeeded(payload.optJSONObject("timingPolicy"))
      val postAnchor = anchorPolicy.optJSONObject("postAnchor")
      val postResult = observePostState(requestId, runId, observerSpec, postAnchor, payload.optJSONObject("postObservePolicy"))
      postResult.pageState?.let { emitWorkflowEvent("page.observed", requestId, runId, it.pageContext) }
      postResult.anchorOutput?.let { emitWorkflowEvent("anchor.post.checked", requestId, runId, it) }
      val eventData = JSONObject().apply {
        put("workflowId", payload.optString("workflowId", "workflow-step"))
        put("operationId", operationId)
        put("operationKind", operationKind)
      }
      emitWorkflowEvent("workflow.step.succeeded", requestId, runId, eventData)
      sendResponse(response(requestId, runId, command, startedAt, "ok", "workflow-step:$operationKind:ok").toString())
      logInfo("workflow_step_finished", "finished $command", requestId, runId, command)
      logInfo("command_finished", "finished $command", requestId, runId, command)
    } catch (throwable: Throwable) {
      emitWorkflowEvent("workflow.step.failed", requestId, runId, JSONObject().apply {
        put("code", throwable.message ?: "WORKFLOW_STEP_FAILED")
      })
      sendResponse(response(requestId, runId, command, startedAt, "error", throwable.message ?: "WORKFLOW_STEP_FAILED").apply {
        put("error", JSONObject().apply {
          put("code", throwable.message ?: "WORKFLOW_STEP_FAILED")
          put("message", throwable.message ?: "WORKFLOW_STEP_FAILED")
        })
      }.toString())
      logError("workflow_step_failed", throwable.message ?: "workflow step failed", requestId, runId, command)
      logError("command_failed", "$command failed", requestId, runId, command)
    }
  }

  private fun requireMatched(pageState: ObservedPageState, anchor: JSONObject, phase: String): JSONObject {
    val input = copyJson(anchor).apply { put("phase", phase) }
    val result = requireOk(evaluateAnchor(pageState, input), "ANCHOR_EVALUATION_FAILED")
    check(result.optBoolean("matched")) { if (phase == "pre") "PRE_ANCHOR_NOT_MATCHED" else "POST_ANCHOR_NOT_MATCHED" }
    return result
  }

  private fun requireOk(result: JSONObject, fallbackCode: String): JSONObject {
    if (result.getString("status") != "ok") {
      val error = result.optJSONObject("error")
      error(error?.optString("code").orEmpty().ifBlank { fallbackCode })
    }
    return result.optJSONObject("output") ?: JSONObject()
  }

  private fun selectedTarget(filterOutput: JSONObject): JSONObject {
    val selectedRef = filterOutput.optString("selectedTargetRef").ifBlank { error("TARGET_NOT_FOUND") }
    val targets = filterOutput.optJSONArray("targets") ?: JSONArray()
    for (index in 0 until targets.length()) {
      val target = targets.optJSONObject(index) ?: continue
      if (target.optString("targetRef") == selectedRef) return target
    }
    error("TARGET_NOT_FOUND")
  }

  private fun buildOperationPayload(operationPlan: JSONObject, selectedTarget: JSONObject): JSONObject {
    val payload = copyJson(operationPlan.optJSONObject("payload")).apply {
      copyKnownFields(operationPlan, this)
    }
    if (AccessibilityTargeting.pointFromPayload(payload) == null && !payload.has("selector")) {
      val bounds = selectedTarget.optJSONObject("bounds") ?: error("TARGET_BOUNDS_MISSING")
      payload.put("x", (bounds.optInt("left") + bounds.optInt("right")) / 2)
      payload.put("y", (bounds.optInt("top") + bounds.optInt("bottom")) / 2)
    }
    selectedTarget.optString("targetRef").takeIf { it.isNotBlank() }?.let { payload.put("targetRef", it) }
    return payload
  }

  private fun sleepIfNeeded(timingPolicy: JSONObject?) {
    val policy = timingPolicy ?: JSONObject()
    if (policy.optBoolean("enabled", true).not()) return
    val minDelay = policy.optLong("minDelayMs", 400L)
    val maxDelay = policy.optLong("maxDelayMs", 900L)
    if (minDelay <= 0L && maxDelay <= 0L) return
    sleepMs(nextDelayMs(minDelay.coerceAtLeast(0L), maxDelay.coerceAtLeast(minDelay)))
  }

  private fun observePostState(
    requestId: String,
    runId: String,
    observerSpec: JSONObject,
    postAnchor: JSONObject?,
    policyJson: JSONObject?,
  ): PostObserveResult {
    if (postAnchor == null) {
      return PostObserveResult(pageState = observePage(requestId, runId, "observe-page", observerSpec))
    }
    val policy = policyJson ?: JSONObject()
    val maxAttempts = policy.optInt("maxAttempts", 6).coerceAtLeast(1)
    val pollIntervalMs = policy.optLong("pollIntervalMs", 350L).coerceAtLeast(0L)
    repeat(maxAttempts) { index ->
      val observed = observePage(requestId, runId, "observe-page", observerSpec)
      val evaluated = requireOk(evaluateAnchor(observed, copyJson(postAnchor).apply { put("phase", "post") }), "ANCHOR_EVALUATION_FAILED")
      if (evaluated.optBoolean("matched")) {
        return PostObserveResult(pageState = observed, anchorOutput = evaluated)
      }
      if (index < maxAttempts - 1 && pollIntervalMs > 0L) sleepMs(pollIntervalMs)
    }
    error("POST_ANCHOR_NOT_MATCHED")
  }

  private fun emitWorkflowEvent(eventType: String, requestId: String, runId: String, data: JSONObject) {
    emitEvent(eventType, requestId, runId, data)
  }

  private fun copyKnownFields(from: JSONObject, to: JSONObject) {
    listOf("backend", "selector", "x", "y", "text", "direction", "key", "uri", "packageName", "component").forEach { key ->
      from.opt(key)?.let { to.put(key, it) }
    }
  }

  private fun copyJson(json: JSONObject?): JSONObject = json?.let { JSONObject(it.toString()) } ?: JSONObject()

  private data class PostObserveResult(
    val pageState: ObservedPageState?,
    val anchorOutput: JSONObject? = null,
  )

  private fun response(
    requestId: String,
    runId: String,
    command: String,
    startedAt: String,
    status: String,
    message: String,
  ): JSONObject {
    val finishedAt = now()
    val (deviceId, model, androidVersion) = deviceInfo()
    return JSONObject().apply {
      put("protocolVersion", "exp01")
      put("requestId", requestId)
      put("runId", runId)
      put("command", command)
      put("status", status)
      put("startedAt", startedAt)
      put("finishedAt", finishedAt)
      put("durationMs", 0)
      put("device", JSONObject().apply {
        put("deviceId", deviceId)
        put("model", model)
        put("androidVersion", androidVersion)
      })
      put("app", JSONObject().apply {
        put("packageName", "com.flowy.explore")
        put("runtimeVersion", versionName())
      })
      put("artifacts", JSONArray())
      put("error", JSONObject.NULL)
      put("message", message)
    }
  }
}
