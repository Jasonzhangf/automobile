package blocks

import (
	"fmt"
	"time"

	"flowy/services/mac-daemon/src/foundation"
	"flowy/services/mac-daemon/src/proto"
)

// Transport abstracts the daemon's state dependencies for blocks.
// Blocks must not receive *state.ClientSession or *state.AppState directly.
type Transport struct {
	NextSeq         func() int64
	Send            func(cmd proto.CommandEnvelope) error
	RegisterPending func(requestID string) chan proto.ResponseEnvelope
	CancelPending   func(requestID string)
	ClientExists    func(deviceID string) bool
}

// NewCommand creates a CommandEnvelope with auto-injected requestID and runID.
// Blocks must use this instead of constructing proto.CommandEnvelope{} directly.
func NewCommand(tr *Transport, command string, payload map[string]any, opts ...func(*proto.CommandEnvelope)) proto.CommandEnvelope {
	now := time.Now()
	cmd := proto.CommandEnvelope{
		ProtocolVersion: "exp01",
		RequestID:       foundation.NewRequestID(now, tr.NextSeq()),
		RunID:           foundation.NewRunID(now, command, "block"),
		Command:         command,
		SentAt:          now.Format(time.RFC3339),
		TimeoutMs:       15000,
		Payload:         payload,
	}
	for _, opt := range opts {
		opt(&cmd)
	}
	return cmd
}

// CommandRoundtrip sends a command via Transport and waits for the response.
// Replaces old CommandRoundtrip(session *state.ClientSession, app *state.AppState, cmd).
func transportRoundtrip(tr *Transport, cmd proto.CommandEnvelope) (*proto.ResponseEnvelope, error) {
	if cmd.RequestID == "" {
		return nil, fmt.Errorf("requestId is required")
	}
	if cmd.TimeoutMs <= 0 {
		cmd.TimeoutMs = 30000
	}

	pending := tr.RegisterPending(cmd.RequestID)
	defer tr.CancelPending(cmd.RequestID)

	if err := tr.Send(cmd); err != nil {
		return nil, fmt.Errorf("send command failed: %w", err)
	}

	select {
	case response := <-pending:
		return &response, nil
	case <-time.After(time.Duration(cmd.TimeoutMs) * time.Millisecond):
		return nil, fmt.Errorf("command timeout after %dms", cmd.TimeoutMs)
	}
}

// CommandRoundtripWithTransport is the exported version of transportRoundtrip.
func CommandRoundtripWithTransport(tr *Transport, cmd proto.CommandEnvelope) (*proto.ResponseEnvelope, error) {
	return transportRoundtrip(tr, cmd)
}
