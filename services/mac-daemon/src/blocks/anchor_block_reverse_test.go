package blocks

import (
	"testing"
	"time"

	"flowy/services/mac-daemon/src/foundation"
	"flowy/services/mac-daemon/src/proto"
)

// Helper: construct an AnchorResult-like response spec for failure path testing.
func makeAnchorRespWithStatus(status string) *proto.ResponseEnvelope {
	return &proto.ResponseEnvelope{
		ProtocolVersion: "exp01",
		Command:         "dump-ui-tree-root",
		Status:          status,
	}
}

// ANCHOR REVERSE: even on every observe failure, must return final result with
// matched=false (NOT match=true as a default).
func TestAnchorBlockWithTransport_AllObservesFail_MatchedFalse(t *testing.T) {
	resp := makeAnchorRespWithStatus("error")
	tr := newTestTransportErr(nil, resp)
	spec := foundation.AnchorSpec{MustContainTexts: []string{"Search"}}
	cfg := AnchorConfig{
		MaxRetries:       2,
		ObserveTimeoutMs: 1000,
		PollDelayFn:      func() time.Duration { return 0 },
	}
	result, err := anchorBlockWithTransport(tr, "/tmp/nonexistent", spec, cfg)
	if err != nil {
		// Either way (error or not), matched MUST be false.
		if result != nil && result.Matched {
			t.Fatalf("expected matched=false on observe failure, got true")
		}
		return
	}
	// No error returned but result.Matched must be false
	if result.Matched {
		t.Fatalf("expected matched=false, got true")
	}
}

// ANCHOR REVERSE: default config must NOT use unbounded retries.
func TestDefaultAnchorConfig_MaxRetriesBounded(t *testing.T) {
	cfg := DefaultAnchorConfig()
	if cfg.MaxRetries <= 0 || cfg.MaxRetries > 100 {
		t.Fatalf("default MaxRetries must be bounded positive, got %d", cfg.MaxRetries)
	}
	if cfg.ObserveTimeoutMs <= 0 || cfg.ObserveTimeoutMs > 60000 {
		t.Fatalf("default ObserveTimeoutMs must be bounded, got %d", cfg.ObserveTimeoutMs)
	}
	if cfg.PollDelayFn == nil {
		t.Fatal("default PollDelayFn must NOT be nil")
	}
}

// ANCHOR REVERSE: result.AttemptCount must NOT exceed configured MaxRetries.
func TestAnchorBlockWithTransport_AttemptCountBounded(t *testing.T) {
	resp := makeAnchorRespWithStatus("error")
	tr := newTestTransportErr(nil, resp)
	spec := foundation.AnchorSpec{MustContainTexts: []string{"Search"}}
	cfg := AnchorConfig{
		MaxRetries:       3,
		ObserveTimeoutMs: 1000,
		PollDelayFn:      func() time.Duration { return 0 },
	}
	result, _ := anchorBlockWithTransport(tr, "/tmp/nonexistent", spec, cfg)
	if result == nil {
		return
	}
	if result.AttemptCount > cfg.MaxRetries {
		t.Fatalf("AttemptCount %d must not exceed configured MaxRetries %d",
			result.AttemptCount, cfg.MaxRetries)
	}
}
