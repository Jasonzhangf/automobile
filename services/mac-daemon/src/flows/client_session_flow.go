package flows

import (
	"encoding/json"
	"time"

	"github.com/gorilla/websocket"

	"flowy/services/mac-daemon/src/foundation"
	"flowy/services/mac-daemon/src/proto"
	"flowy/services/mac-daemon/src/state"
)

func RunClientSession(app *state.AppState, conn *websocket.Conn) {
	defer conn.Close()

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

	for {
		_, raw, readErr := conn.ReadMessage()
		if readErr != nil {
			return
		}
		var envelope map[string]json.RawMessage
		if json.Unmarshal(raw, &envelope) != nil {
			continue
		}
		if helloRaw, ok := envelope["type"]; ok {
			var messageType string
			if json.Unmarshal(helloRaw, &messageType) == nil && messageType == "client_hello" {
				nextHello, decodeErr := foundation.Decode[proto.ClientHello](raw)
				if decodeErr == nil && nextHello.ProtocolVersion == "exp01" && nextHello.DeviceID == hello.DeviceID {
					app.UpdateClientHello(hello.DeviceID, nextHello)
				}
				continue
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
