package blocks

import (
	"runtime"
	"testing"
	"time"

	"flowy/services/mac-daemon/src/proto"
)

// RECOVERY REVERSE: empty ClientExists must NOT return true early.
func TestWaitForReconnectTransport_AlwaysFalse_Exits(t *testing.T) {
	tr := &Transport{
		NextSeq:         func() int64 { return 1 },
		Send:            func(cmd proto.CommandEnvelope) error { return nil },
		RegisterPending: func(requestID string) chan proto.ResponseEnvelope { return nil },
		CancelPending:   func(requestID string) {},
		ClientExists:    func(deviceID string) bool { return false },
	}
	if WaitForReconnectTransport(tr, "unknown-device", 100) {
		t.Fatal("expected false when ClientExists is always false")
	}
}

// RECOVERY REVERSE: must terminate even with maxWaitMs<=0 — but here we use
// a hard small budget (no expansion to default 30000ms). Lock: the function
// honors the caller-supplied maxWaitMs when positive.
func TestWaitForReconnectTransport_HonorsExplicitBudget(t *testing.T) {
	tr := &Transport{
		NextSeq:         func() int64 { return 1 },
		Send:            func(cmd proto.CommandEnvelope) error { return nil },
		RegisterPending: func(requestID string) chan proto.ResponseEnvelope { return nil },
		CancelPending:   func(requestID string) {},
		ClientExists:    func(deviceID string) bool { return false },
	}
	start := time.Now()
	_ = WaitForReconnectTransport(tr, "none", 80)
	elapsed := time.Since(start)
	if elapsed > 3*time.Second {
		t.Fatalf("explicit maxWaitMs=80ms must keep total elapsed < 3s, got %v", elapsed)
	}
}

// RECOVERY REVERSE: goroutine/runtime sanity — must NOT spawn runaway goroutines.
func TestWaitForReconnectTransport_NoGoroutineLeak(t *testing.T) {
	tr := &Transport{
		NextSeq:         func() int64 { return 1 },
		Send:            func(cmd proto.CommandEnvelope) error { return nil },
		RegisterPending: func(requestID string) chan proto.ResponseEnvelope { return nil },
		CancelPending:   func(requestID string) {},
		ClientExists:    func(deviceID string) bool { return false },
	}
	before := runtime.NumGoroutine()
	_ = WaitForReconnectTransport(tr, "nope", 50)
	runtime.Gosched()
	after := runtime.NumGoroutine()
	// After the function returns, there should be no extra goroutines spawned
	// (the function uses only the calling goroutine for the loop).
	if after > before+1 {
		t.Fatalf("possible goroutine leak: before=%d after=%d", before, after)
	}
}
