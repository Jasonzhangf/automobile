package blocks

import (
	"fmt"
	"time"

	"flowy/services/mac-daemon/src/proto"
	"flowy/services/mac-daemon/src/state"
)

// CommandRoundtrip sends a command to the Android device via WebSocket
// and waits for the response.
//
// Deprecated: Prefer blocks.CommandRoundtrip(tr, cmd) using a Transport.
// This wrapper converts session+app to a Transport internally and delegates.
func CommandRoundtrip(session *state.ClientSession, app *state.AppState, cmd proto.CommandEnvelope) (*proto.ResponseEnvelope, error) {
	if cmd.RequestID == "" {
		return nil, fmt.Errorf("requestId is required")
	}
	if cmd.TimeoutMs <= 0 {
		cmd.TimeoutMs = 30000
	}

	pending := app.RegisterPending(cmd.RequestID)
	defer app.CancelPending(cmd.RequestID)

	if err := sendCommand(session, cmd); err != nil {
		return nil, fmt.Errorf("send command failed: %w", err)
	}

	select {
	case response := <-pending:
		return &response, nil
	case <-time.After(time.Duration(cmd.TimeoutMs) * time.Millisecond):
		return nil, fmt.Errorf("command timeout after %dms", cmd.TimeoutMs)
	}
}

// makeTransport constructs a Transport from a session + app + artifactRoot.
// Blocks should eventually accept Transport directly instead of session+app.
func makeTransport(session *state.ClientSession, app *state.AppState) *Transport {
	return &Transport{
		NextSeq:         func() int64 { return app.NextRequestSeq() },
		Send:            func(cmd proto.CommandEnvelope) error { return sendCommand(session, cmd) },
		RegisterPending: func(requestID string) chan proto.ResponseEnvelope { return app.RegisterPending(requestID) },
		CancelPending:   func(requestID string) { app.CancelPending(requestID) },
		ClientExists:    func(deviceID string) bool { _, ok := app.Client(deviceID); return ok },
	}
}

// sendCommand writes a command to the session's WebSocket connection.
// Not exported; blocks use CommandRoundtrip or Transport.Send instead.
func sendCommand(session *state.ClientSession, command proto.CommandEnvelope) error {
	session.SendMu.Lock()
	defer session.SendMu.Unlock()
	return session.Conn.WriteJSON(command)
}

// SendCommand writes a command to the session's WebSocket connection.
// Deprecated: Prefer Transport.Send.
func SendCommand(session *state.ClientSession, command proto.CommandEnvelope) error {
	return sendCommand(session, command)
}

// MakeTransport constructs a Transport from a session + app.
// Exported so flows can build a Transport without accessing state directly.
func MakeTransport(session *state.ClientSession, app *state.AppState) *Transport {
	return makeTransport(session, app)
}
