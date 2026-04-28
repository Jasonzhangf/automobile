package com.flowy.explore.foundation

import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
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
  /** Monotonically increasing generation. Each connect() increments it.
   *  Callbacks from a socket whose generation doesn't match current are ignored. */
  private val generation = AtomicInteger(0)

  fun connect(url: String) {
    val gen = generation.incrementAndGet()
    val old = socket
    socket = null
    old?.close(1000, "reconnect")
    val request = Request.Builder().url(url).build()
    socket = client.newWebSocket(request, object : WebSocketListener() {
      override fun onOpen(webSocket: WebSocket, response: Response) {
        if (generation.get() == gen) onOpen()
      }
      override fun onMessage(webSocket: WebSocket, text: String) {
        if (generation.get() == gen) onMessage(text)
      }
      override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
        if (generation.get() == gen) onClosing()
      }
      override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
        if (generation.get() == gen) onFailure(t)
      }
    })
  }

  fun send(text: String): Boolean = socket?.send(text) ?: false

  fun close() {
    socket?.close(1000, "client-close")
    socket = null
  }
}
