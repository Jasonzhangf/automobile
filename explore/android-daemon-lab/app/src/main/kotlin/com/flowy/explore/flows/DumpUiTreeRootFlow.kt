package com.flowy.explore.flows

import android.os.Build
import com.flowy.explore.blocks.AppendLogBlock
import com.flowy.explore.blocks.DumpUiTreeRootBlock
import com.flowy.explore.blocks.UploadArtifactBlock
import com.flowy.explore.foundation.DisplayInfoReader
import com.flowy.explore.foundation.TimeHelper
import com.flowy.explore.foundation.VersionReader
import com.flowy.explore.foundation.WsClientAdapter
import org.json.JSONArray
import org.json.JSONObject

class DumpUiTreeRootFlow(
  private val appendLogBlock: AppendLogBlock,
  private val versionReader: VersionReader,
  private val displayInfoReader: DisplayInfoReader,
  private val dumpUiTreeRootBlock: DumpUiTreeRootBlock,
  private val uploadArtifactBlock: UploadArtifactBlock,
  private val wsClientAdapter: WsClientAdapter,
) {
  fun run(requestId: String, runId: String, command: String) {
    val startedAt = TimeHelper.now()
    appendLogBlock.info("dump_ui_tree_root_started", "dumping root ui tree", requestId, runId, command)
    try {
      val snapshot = dumpUiTreeRootBlock.run()
      val displayInfo = displayInfoReader.read()
      val nodesJson = JSONObject(snapshot.rawJson)
      val nodeCount = nodesJson.optJSONArray("nodes")?.length() ?: 0
      val pageContext = JSONObject().apply {
        put("requestId", requestId)
        put("runId", runId)
        put("command", command)
        put("capturedAt", snapshot.capturedAt)
        put("app", JSONObject().apply {
          put("packageName", snapshot.packageName ?: JSONObject.NULL)
        })
        put("screen", JSONObject().apply {
          put("widthPx", displayInfo.widthPx)
          put("heightPx", displayInfo.heightPx)
          put("rotation", displayInfo.rotation)
          put("densityDpi", displayInfo.densityDpi)
        })
        put("runtime", JSONObject().apply {
          put("projectionReady", false)
          put("accessibilityReady", false)
          put("rootReady", true)
          put("rootUiDumpReady", true)
        })
      }
      val artifacts = JSONArray().apply {
        put(upload(requestId, runId, command, "root-ui-tree", "root-ui-tree.json", snapshot.rawJson.toByteArray()))
        put(upload(requestId, runId, command, "page-context", "page-context.json", pageContext.toString(2).toByteArray()))
      }
      appendLogBlock.info("dump_ui_tree_root_finished", "dumped $nodeCount nodes", requestId, runId, command)
      wsClientAdapter.send(baseResponse(requestId, runId, command, startedAt).apply {
        put("status", "ok")
        put("artifacts", artifacts)
        put("error", JSONObject.NULL)
        put("message", "dump-ui-tree-root:$nodeCount")
      }.toString())
      appendLogBlock.info("command_finished", "finished $command", requestId, runId, command)
    } catch (throwable: Throwable) {
      appendLogBlock.error("dump_ui_tree_root_failed", throwable.message ?: "dump ui tree root failed", requestId, runId, command)
      wsClientAdapter.send(baseResponse(requestId, runId, command, startedAt).apply {
        put("status", "error")
        put("artifacts", JSONArray())
        put("error", JSONObject().apply {
          put("code", throwable.message ?: "DUMP_UI_TREE_ROOT_FAILED")
          put("message", throwable.message ?: "dump ui tree root failed")
        })
        put("message", "dump-ui-tree-root-error")
      }.toString())
      appendLogBlock.error("command_failed", "$command failed", requestId, runId, command)
    }
  }

  private fun upload(requestId: String, runId: String, command: String, kind: String, fileName: String, content: ByteArray): JSONObject {
    appendLogBlock.info("artifact_upload_started", "uploading $fileName", requestId, runId, command)
    val stored = uploadArtifactBlock.run(JSONObject().apply {
      put("protocolVersion", "exp01")
      put("requestId", requestId)
      put("runId", runId)
      put("deviceId", Build.DEVICE)
      put("command", command)
      put("kind", kind)
      put("fileName", fileName)
      put("contentType", "application/json")
    }, content)
    appendLogBlock.info("artifact_upload_finished", "uploaded $fileName", requestId, runId, command)
    return stored
  }

  private fun baseResponse(requestId: String, runId: String, command: String, startedAt: String): JSONObject {
    return JSONObject().apply {
      put("protocolVersion", "exp01")
      put("requestId", requestId)
      put("runId", runId)
      put("command", command)
      put("startedAt", startedAt)
      put("finishedAt", TimeHelper.now())
      put("durationMs", 0)
      put("device", JSONObject().apply {
        put("deviceId", Build.DEVICE)
        put("model", Build.MODEL)
        put("androidVersion", Build.VERSION.RELEASE)
      })
      put("app", JSONObject().apply {
        put("packageName", "com.flowy.explore")
        put("runtimeVersion", versionReader.versionName())
      })
    }
  }
}
