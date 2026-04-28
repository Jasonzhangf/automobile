package flows

import (
	"encoding/json"
	"log"
	"time"

	"github.com/gorilla/websocket"

	"flowy/services/mac-daemon/src/foundation"
	"flowy/services/mac-daemon/src/proto"
	"flowy/services/mac-daemon/src/state"
)

const (
	// readDeadline is the max time we wait for ANY WS activity (data, ping, pong)
	// before considering the connection dead. OkHttp sends pings every 15s, and
	// gorilla auto-replies with pong, so TCP stays alive. This is a safety net.
	readDeadline = 180 * time.Second

	// keepaliveInterval is how often we send an app-level keepalive text frame.
	// This triggers Android's onMessage → markAlive(), preventing the watchdog
	// from killing a healthy idle connection.
	keepaliveInterval = 25 * time.Second
)

func RunClientSession(app *state.AppState, conn *websocket.Conn) {
	defer conn.Close()

	conn.SetReadDeadline(time.Now().Add(readDeadline))
	conn.SetPongHandler(func(string) error {
		conn.SetReadDeadline(time.Now().Add(readDeadline))
		return nil
	})
	// OkHttp sends WS Ping every 15s. gorilla auto-replies Pong but
	// ReadMessage doesn't return for pings, so we need a PingHandler
	// to reset the deadline. Without this, the 180s readDeadline fires.
	conn.SetPingHandler(func(appData string) error {
		conn.SetReadDeadline(time.Now().Add(readDeadline))
		// Send pong back (gorilla's default behavior, must replicate)
		deadline := time.Now().Add(10 * time.Second)
		_ = conn.WriteControl(websocket.PongMessage, []byte(appData), deadline)
		return nil
	})

	// Read hello
	_, data, err := conn.ReadMessage()
	if err != nil {
		return
	}

	hello, err := foundation.Decode[proto.ClientHello](data)
	if err != nil || hello.Type != "client_hello" || hello.ProtocolVersion != "exp01" || hello.DeviceID == "" {
		_ = conn.WriteJSON(proto.ErrorObject{Code: "INVALID_HELLO", Message: "invalid client_hello"})
		return
	}

	session := &state.ClientSession{Hello: hello, Conn: conn, ConnectedAt: time.Now()}
	app.RegisterClient(session)
	defer app.RemoveClient(hello.DeviceID)
	log.Printf("ws connected: %s (v%s)", hello.DeviceID, hello.RuntimeVersion)

	// Start keepalive goroutine: sends app-level text frame through SendMu
	// to avoid racing with command writes.
	done := make(chan struct{})
	go func() {
		ticker := time.NewTicker(keepaliveInterval)
		defer ticker.Stop()
		for {
			select {
			case <-ticker.C:
				session.SendMu.Lock()
				writeErr := conn.WriteMessage(websocket.TextMessage, []byte(`{"type":"keepalive"}`))
				session.SendMu.Unlock()
				if writeErr != nil {
					log.Printf("ws keepalive send failed: %v", writeErr)
					return
				}
			case <-done:
				return
			}
		}
	}()
	defer close(done)

	for {
		_, raw, readErr := conn.ReadMessage()
		if readErr != nil {
			log.Printf("ws disconnected: %s (%v)", hello.DeviceID, readErr)
			return
		}
		conn.SetReadDeadline(time.Now().Add(readDeadline))

		var envelope map[string]json.RawMessage
		if json.Unmarshal(raw, &envelope) != nil {
			continue
		}
		// Ignore keepalive frames from client
		if t, ok := envelope["type"]; ok {
			var messageType string
			if json.Unmarshal(t, &messageType) == nil {
				if messageType == "keepalive" {
					continue
				}
				if messageType == "client_hello" {
					nextHello, decodeErr := foundation.Decode[proto.ClientHello](raw)
					if decodeErr == nil && nextHello.ProtocolVersion == "exp01" && nextHello.DeviceID == hello.DeviceID {
						app.UpdateClientHello(hello.DeviceID, nextHello)
					}
					continue
				}
			}
		}
		if _, ok := envelope["requestId"]; !ok {
			continue
		}
		response, decodeErr := foundation.Decode[proto.ResponseEnvelope](raw)
		if decodeErr != nil {
			continue
		}
		app.ResolvePending(response)
	}
}
