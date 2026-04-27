package com.flowy.explore.foundation

import java.util.concurrent.TimeUnit
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener

class WsClientAdapter(
  private val client: OkHttpClient = OkHttpClient.Builder()
    .pingInterval(15, TimeUnit.SECONDS)
    .retryOnConnectionFailure(true)
    .build(),
  private val onOpen: () -> Unit,
  private val onMessage: (String) -> Unit,
  private val onClosing: () -> Unit,
  private val onFailure: (Throwable) -> Unit,
) {
  private var socket: WebSocket? = null

  @Volatile private var suppressCloseCallback = false

  fun connect(url: String) {
    suppressCloseCallback = true
    socket?.close(1000, "reconnect")
    socket = null
    suppressCloseCallback = false
    val request = Request.Builder().url(url).build()
    socket = client.newWebSocket(request, object : WebSocketListener() {
      override fun onOpen(webSocket: WebSocket, response: Response) = onOpen()
      override fun onMessage(webSocket: WebSocket, text: String) = onMessage(text)
      override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
        if (!suppressCloseCallback) onClosing()
      }
      override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) = onFailure(t)
    })
  }

  fun send(text: String): Boolean = socket?.send(text) ?: false

  fun close() {
    socket?.close(1000, "client-close")
    socket = null
  }
}
