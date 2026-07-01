package blocks

import (
	"testing"

	"flowy/services/mac-daemon/src/proto"
)

func TestNewCommand(t *testing.T) {
	tr := &Transport{
		NextSeq:         func() int64 { return 42 },
		Send:            func(cmd proto.CommandEnvelope) error { return nil },
		RegisterPending: func(requestID string) chan proto.ResponseEnvelope { return nil },
		CancelPending:   func(requestID string) {},
		ClientExists:    func(deviceID string) bool { return false },
	}

	cmd := NewCommand(tr, "tap", map[string]any{"x": 100, "y": 200})
	if cmd.ProtocolVersion != "exp01" {
		t.Errorf("expected protocolVersion=exp01, got %q", cmd.ProtocolVersion)
	}
	if cmd.Command != "tap" {
		t.Errorf("expected command=tap, got %q", cmd.Command)
	}
	if cmd.RequestID == "" {
		t.Error("expected non-empty RequestID")
	}
	if cmd.RunID == "" {
		t.Error("expected non-empty RunID")
	}
	if cmd.TimeoutMs != 15000 {
		t.Errorf("expected default timeoutMs=15000, got %d", cmd.TimeoutMs)
	}
	if p, ok := cmd.Payload.(map[string]any); ok && p["x"] != 100 {
		t.Errorf("expected payload[x]=100, got %v", p["x"])
	}
}

func TestNewCommand_WithOptions(t *testing.T) {
	tr := &Transport{
		NextSeq:         func() int64 { return 1 },
		Send:            func(cmd proto.CommandEnvelope) error { return nil },
		RegisterPending: func(requestID string) chan proto.ResponseEnvelope { return nil },
		CancelPending:   func(requestID string) {},
		ClientExists:    func(deviceID string) bool { return false },
	}

	cmd := NewCommand(tr, "scroll", map[string]any{"direction": "forward"},
		func(c *proto.CommandEnvelope) { c.TimeoutMs = 30000 },
	)
	if cmd.TimeoutMs != 30000 {
		t.Errorf("expected timeoutMs=30000 (from opt), got %d", cmd.TimeoutMs)
	}
}

func TestNewCommand_NextSeqCalled(t *testing.T) {
	calls := 0
	tr := &Transport{
		NextSeq: func() int64 {
			calls++
			return int64(calls)
		},
		Send:            func(cmd proto.CommandEnvelope) error { return nil },
		RegisterPending: func(requestID string) chan proto.ResponseEnvelope { return nil },
		CancelPending:   func(requestID string) {},
		ClientExists:    func(deviceID string) bool { return false },
	}

	for i := 0; i < 5; i++ {
		_ = NewCommand(tr, "ping", map[string]any{})
	}
	if calls != 5 {
		t.Errorf("expected NextSeq called 5 times, got %d", calls)
	}
}

func TestCommandRoundtripWithTransport_Success(t *testing.T) {
	expectedResp := proto.ResponseEnvelope{
		ProtocolVersion: "exp01",
		RequestID:       "req-1-abc-1700000000",
		Command:         "ping",
		Status:          "ok",
	}
	respCh := make(chan proto.ResponseEnvelope, 1)
	respCh <- expectedResp

	tr := &Transport{
		NextSeq: func() int64 { return 1 },
		Send:    func(cmd proto.CommandEnvelope) error { return nil },
		RegisterPending: func(requestID string) chan proto.ResponseEnvelope {
			return respCh
		},
		CancelPending: func(requestID string) {},
		ClientExists:  func(deviceID string) bool { return true },
	}

	cmd := NewCommand(tr, "ping", map[string]any{})
	cmd.TimeoutMs = 1000

	resp, err := CommandRoundtripWithTransport(tr, cmd)
	if err != nil {
		t.Fatalf("expected no error, got: %v", err)
	}
	if resp.Status != "ok" {
		t.Errorf("expected status=ok, got %q", resp.Status)
	}
}

func TestCommandRoundtripWithTransport_Timeout(t *testing.T) {
	respCh := make(chan proto.ResponseEnvelope, 1) // empty

	tr := &Transport{
		NextSeq: func() int64 { return 1 },
		Send:    func(cmd proto.CommandEnvelope) error { return nil },
		RegisterPending: func(requestID string) chan proto.ResponseEnvelope {
			return respCh
		},
		CancelPending: func(requestID string) {},
		ClientExists:  func(deviceID string) bool { return true },
	}

	cmd := NewCommand(tr, "ping", map[string]any{})
	cmd.TimeoutMs = 100 // very short timeout

	_, err := CommandRoundtripWithTransport(tr, cmd)
	if err == nil {
		t.Fatal("expected timeout error, got nil")
	}
}

func TestCommandRoundtripWithTransport_MissingRequestID(t *testing.T) {
	tr := &Transport{
		NextSeq: func() int64 { return 1 },
		Send:    func(cmd proto.CommandEnvelope) error { return nil },
		RegisterPending: func(requestID string) chan proto.ResponseEnvelope {
			return make(chan proto.ResponseEnvelope, 1)
		},
		CancelPending: func(requestID string) {},
		ClientExists:  func(deviceID string) bool { return true },
	}

	cmd := proto.CommandEnvelope{
		ProtocolVersion: "exp01",
		RequestID:       "", // missing
		Command:         "ping",
		TimeoutMs:       1000,
	}
	_, err := CommandRoundtripWithTransport(tr, cmd)
	if err == nil {
		t.Fatal("expected error for missing requestId, got nil")
	}
}

func TestTransport_ClientExists(t *testing.T) {
	tr := &Transport{
		NextSeq:         func() int64 { return 1 },
		Send:            func(cmd proto.CommandEnvelope) error { return nil },
		RegisterPending: func(requestID string) chan proto.ResponseEnvelope { return nil },
		CancelPending:   func(requestID string) {},
		ClientExists:    func(deviceID string) bool { return deviceID == "OP64DDL1" },
	}

	if !tr.ClientExists("OP64DDL1") {
		t.Error("expected ClientExists=OP64DDL1 to be true")
	}
	if tr.ClientExists("unknown") {
		t.Error("expected ClientExists=unknown to be false")
	}
}
