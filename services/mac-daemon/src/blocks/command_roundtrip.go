package blocks

import (
	"fmt"
	"time"

	"flowy/services/mac-daemon/src/proto"
	"flowy/services/mac-daemon/src/state"
)

// CommandRoundtrip sends a command to the Android device via WebSocket
// and waits for the response. It does NOT finalize the run or persist artifacts —
// that responsibility belongs to the caller.
func CommandRoundtrip(session *state.ClientSession, app *state.AppState, cmd proto.CommandEnvelope) (*proto.ResponseEnvelope, error) {
	if cmd.RequestID == "" {
		return nil, fmt.Errorf("requestId is required")
	}
	if cmd.TimeoutMs <= 0 {
		cmd.TimeoutMs = 30000
	}

	pending := app.RegisterPending(cmd.RequestID)
	defer app.CancelPending(cmd.RequestID)

	if err := SendCommand(session, cmd); err != nil {
		return nil, fmt.Errorf("send command failed: %w", err)
	}

	select {
	case response := <-pending:
		return &response, nil
	case <-time.After(time.Duration(cmd.TimeoutMs) * time.Millisecond):
		return nil, fmt.Errorf("command timeout after %dms", cmd.TimeoutMs)
	}
}
