package com.flowy.explore.foundation

import org.json.JSONArray
import org.json.JSONObject

object JsonCodec {
  fun obj(block: JSONObject.() -> Unit): JSONObject = JSONObject().apply(block)

  fun array(values: Iterable<JSONObject>): JSONArray = JSONArray().apply {
    values.forEach { put(it) }
  }
}
