package com.flowy.explore.blocks

import com.flowy.explore.foundation.WsClientAdapter

class ConnectWsBlock(private val wsClientAdapter: WsClientAdapter) {
  fun run(url: String) = wsClientAdapter.connect(url)
}
