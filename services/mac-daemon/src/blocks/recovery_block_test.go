package blocks

import (
	"errors"
	"testing"

	"flowy/services/mac-daemon/src/proto"
	"flowy/services/mac-daemon/src/state"
)

func TestHealthCheckWithTransport_Success(t *testing.T) {
	tr := &Transport{
		NextSeq: func() int64 { return 1 },
		Send:    func(cmd proto.CommandEnvelope) error { return nil },
		RegisterPending: func(requestID string) chan proto.ResponseEnvelope {
			ch := make(chan proto.ResponseEnvelope, 1)
			ch <- proto.ResponseEnvelope{
				ProtocolVersion: "exp01",
				RequestID:       requestID,
				Command:         "ping",
				Status:          "ok",
			}
			return ch
		},
		CancelPending: func(requestID string) {},
		ClientExists:  func(deviceID string) bool { return true },
	}
	if err := healthCheckWithTransport(tr, 1000); err != nil {
		t.Errorf("expected no error, got: %v", err)
	}
}

func TestHealthCheckWithTransport_ErrorResponse(t *testing.T) {
	tr := &Transport{
		NextSeq: func() int64 { return 1 },
		Send:    func(cmd proto.CommandEnvelope) error { return nil },
		RegisterPending: func(requestID string) chan proto.ResponseEnvelope {
			ch := make(chan proto.ResponseEnvelope, 1)
			ch <- proto.ResponseEnvelope{
				ProtocolVersion: "exp01",
				RequestID:       requestID,
				Command:         "ping",
				Status:          "error",
				Error:           &proto.ErrorObject{Code: "DEVICE_OFFLINE", Message: "device not responding"},
			}
			return ch
		},
		CancelPending: func(requestID string) {},
		ClientExists:  func(deviceID string) bool { return true },
	}
	if err := healthCheckWithTransport(tr, 1000); err == nil {
		t.Error("expected error for error response")
	}
}

func TestHealthCheckWithTransport_SendError(t *testing.T) {
	tr := &Transport{
		NextSeq: func() int64 { return 1 },
		Send:    func(cmd proto.CommandEnvelope) error { return errors.New("send failed") },
		RegisterPending: func(requestID string) chan proto.ResponseEnvelope {
			return make(chan proto.ResponseEnvelope, 1)
		},
		CancelPending: func(requestID string) {},
		ClientExists:  func(deviceID string) bool { return true },
	}
	if err := healthCheckWithTransport(tr, 1000); err == nil {
		t.Error("expected error for send failure")
	}
}

func TestDefaultRecoveryConfig(t *testing.T) {
	cfg := DefaultRecoveryConfig()
	if cfg.MaxRetries != 3 {
		t.Errorf("expected MaxRetries=3, got %d", cfg.MaxRetries)
	}
	if cfg.PingTimeout != 8000 {
		t.Errorf("expected PingTimeout=8000, got %d", cfg.PingTimeout)
	}
}

func TestWaitForReconnectTransport_NotConnected(t *testing.T) {
	tr := &Transport{
		NextSeq:         func() int64 { return 1 },
		Send:            func(cmd proto.CommandEnvelope) error { return nil },
		RegisterPending: func(requestID string) chan proto.ResponseEnvelope { return nil },
		CancelPending:   func(requestID string) {},
		ClientExists:    func(deviceID string) bool { return false },
	}
	// timeout 200ms - should return false quickly
	if WaitForReconnectTransport(tr, "OP64DDL1", 200) {
		t.Error("expected false (not reconnected)")
	}
}

func TestWaitForReconnectTransport_Connected(t *testing.T) {
	tr := &Transport{
		NextSeq:         func() int64 { return 1 },
		Send:            func(cmd proto.CommandEnvelope) error { return nil },
		RegisterPending: func(requestID string) chan proto.ResponseEnvelope { return nil },
		CancelPending:   func(requestID string) {},
		ClientExists:    func(deviceID string) bool { return true },
	}
	if !WaitForReconnectTransport(tr, "OP64DDL1", 1000) {
		t.Error("expected true (already connected)")
	}
}

// Ensure state import is used to avoid unused error
var _ = state.New
