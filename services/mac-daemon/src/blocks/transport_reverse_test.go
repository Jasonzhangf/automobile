package blocks

import (
	"errors"
	"testing"

	"flowy/services/mac-daemon/src/proto"
)

// --- Reverse tests: verify what will NOT happen ---

func TestCommandRoundtripWithTransport_NilPendingChannel_DoesNotPanic(t *testing.T) {
	tr := &Transport{
		NextSeq: func() int64 { return 1 },
		Send:    func(cmd proto.CommandEnvelope) error { return nil },
		RegisterPending: func(requestID string) chan proto.ResponseEnvelope {
			return nil // nil channel — should not panic
		},
		CancelPending: func(requestID string) {},
		ClientExists:  func(deviceID string) bool { return true },
	}

	cmd := proto.CommandEnvelope{
		ProtocolVersion: "exp01",
		RequestID:       "req-nil-1",
		Command:         "ping",
		TimeoutMs:       50,
	}

	_, err := CommandRoundtripWithTransport(tr, cmd)
	if err == nil {
		t.Error("expected timeout error on nil channel, got nil")
	}
}

func TestNewCommand_NilPayload_StillCreatesEnvelope(t *testing.T) {
	tr := &Transport{
		NextSeq:         func() int64 { return 1 },
		Send:            func(cmd proto.CommandEnvelope) error { return nil },
		RegisterPending: func(requestID string) chan proto.ResponseEnvelope { return nil },
		CancelPending:   func(requestID string) {},
		ClientExists:    func(deviceID string) bool { return true },
	}

	cmd := NewCommand(tr, "ping", nil)
	if cmd.Command != "ping" {
		t.Errorf("expected command=ping, got %q", cmd.Command)
	}
	// nil payload → NewCommand wraps into map; this documents actual behavior
	if cmd.RequestID == "" {
		t.Error("RequestID should be non-empty even with nil payload")
	}
}

func TestCommandRoundtripWithTransport_SendError_Propagates(t *testing.T) {
	tr := &Transport{
		NextSeq: func() int64 { return 1 },
		Send: func(cmd proto.CommandEnvelope) error {
			return errors.New("transport closed")
		},
		RegisterPending: func(requestID string) chan proto.ResponseEnvelope {
			return make(chan proto.ResponseEnvelope, 1)
		},
		CancelPending: func(requestID string) {},
		ClientExists:  func(deviceID string) bool { return true },
	}

	cmd := NewCommand(tr, "tap", map[string]any{"x": 100})
	_, err := CommandRoundtripWithTransport(tr, cmd)
	if err == nil {
		t.Error("expected error when Send returns error, got nil")
	}
}

func TestCommandRoundtripWithTransport_ImmediateClose(t *testing.T) {
	tr := &Transport{
		NextSeq: func() int64 { return 1 },
		Send:    func(cmd proto.CommandEnvelope) error { return nil },
		RegisterPending: func(requestID string) chan proto.ResponseEnvelope {
			ch := make(chan proto.ResponseEnvelope, 1)
			close(ch) // immediately closed — should timeout or return zero value
			return ch
		},
		CancelPending: func(requestID string) {},
		ClientExists:  func(deviceID string) bool { return true },
	}

	cmd := NewCommand(tr, "ping", map[string]any{})
	cmd.TimeoutMs = 200
	resp, err := CommandRoundtripWithTransport(tr, cmd)
	// Closed channel delivers immediately — resp may be zero-value
	if err == nil && resp.Status == "" {
		t.Log("closed channel returned zero-value response (expected behavior)")
	}
}
