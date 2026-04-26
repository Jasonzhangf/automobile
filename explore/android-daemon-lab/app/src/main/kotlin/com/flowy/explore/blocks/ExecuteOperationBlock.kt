package com.flowy.explore.blocks

import com.flowy.explore.foundation.BlockResultFactory
import com.flowy.explore.foundation.TimeHelper
import org.json.JSONObject

class ExecuteOperationBlock(
  private val tap: (JSONObject) -> String,
  private val scroll: (JSONObject) -> String,
  private val inputText: (JSONObject) -> String,
  private val back: () -> String,
  private val pressKey: (JSONObject) -> String,
  private val now: () -> String = TimeHelper::now,
) {
  constructor(
    tapBlock: TapBlock,
    scrollBlock: ScrollBlock,
    inputTextBlock: InputTextBlock,
    backBlock: BackBlock,
    pressKeyBlock: PressKeyBlock,
  ) : this(
    tap = tapBlock::run,
    scroll = scrollBlock::run,
    inputText = inputTextBlock::run,
    back = backBlock::run,
    pressKey = pressKeyBlock::run,
  )

  fun run(operation: String, payload: JSONObject = JSONObject(), operationId: String = operation): JSONObject {
    val startedAt = now()
    return try {
      val message = when (operation) {
        "tap" -> tap(payload)
        "scroll" -> scroll(payload)
        "input-text" -> inputText(payload)
        "back" -> back()
        "press-key" -> pressKey(payload)
        else -> error("UNSUPPORTED_OPERATION")
      }
      BlockResultFactory.ok(
        startedAt = startedAt,
        output = JSONObject().apply {
          put("operationId", operationId)
          put("status", "ok")
          put("message", message)
        },
      )
    } catch (throwable: Throwable) {
      val code = throwable.message ?: "OPERATION_FAILED"
      BlockResultFactory.error(startedAt = startedAt, code = code, message = code)
    }
  }
}
