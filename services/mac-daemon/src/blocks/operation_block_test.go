package blocks

import (
	"testing"

	"flowy/services/mac-daemon/src/proto"
)

func newTestTransport(sendErr error) *Transport {
	return &Transport{
		NextSeq: func() int64 { return 1 },
		Send:    func(cmd proto.CommandEnvelope) error { return sendErr },
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
}

func TestTapWithTransport_Success(t *testing.T) {
	tr := newTestTransport(nil)
	x, y := 100, 200
	err := tapWithTransport(tr, "", TapTarget{X: &x, Y: &y}, BackendAccessibility, 1000)
	if err != nil {
		t.Fatalf("expected no error, got: %v", err)
	}
}

func TestTapWithTransport_DirectCoords(t *testing.T) {
	tr := newTestTransport(nil)
	x, y := 500, 800
	err := tapWithTransport(tr, "", TapTarget{X: &x, Y: &y}, BackendRoot, 1000)
	if err != nil {
		t.Fatalf("expected no error, got: %v", err)
	}
}

func TestScrollWithTransport_Success(t *testing.T) {
	tr := newTestTransport(nil)
	x, y := 540, 1200
	err := scrollWithTransport(tr, "", ScrollTarget{X: &x, Y: &y}, "forward", BackendRoot, 1000)
	if err != nil {
		t.Fatalf("expected no error, got: %v", err)
	}
}

func TestScrollWithTransport_DefaultDirection(t *testing.T) {
	tr := newTestTransport(nil)
	x, y := 540, 1200
	// empty direction should default to "forward"
	err := scrollWithTransport(tr, "", ScrollTarget{X: &x, Y: &y}, "", BackendRoot, 1000)
	if err != nil {
		t.Fatalf("expected no error, got: %v", err)
	}
}

func TestOperationBackend_Constants(t *testing.T) {
	if string(BackendAccessibility) != "accessibility" {
		t.Errorf("BackendAccessibility should be 'accessibility', got %q", BackendAccessibility)
	}
	if string(BackendRoot) != "root" {
		t.Errorf("BackendRoot should be 'root', got %q", BackendRoot)
	}
}

func TestResolveTapPoint_DirectCoords(t *testing.T) {
	tr := newTestTransport(nil)
	x, y := 100, 200
	gotX, gotY, err := resolveTapPoint(tr, "", TapTarget{X: &x, Y: &y}, 1000)
	if err != nil {
		t.Fatalf("expected no error, got: %v", err)
	}
	if gotX != 100 || gotY != 200 {
		t.Errorf("expected (100, 200), got (%d, %d)", gotX, gotY)
	}
}

func TestResolveTapPoint_NoCoordsNoSnapshot(t *testing.T) {
	tr := newTestTransport(nil)
	// No x/y, no snapshot -> error
	_, _, err := resolveTapPoint(tr, "", TapTarget{DescContains: "search"}, 1000)
	if err == nil {
		t.Fatal("expected error when no coords and no snapshot available")
	}
}

func TestFindNodeBySelector_Empty(t *testing.T) {
	// Empty list - no match
	n := findNodeBySelector(nil, "desc", "text")
	if n != nil {
		t.Error("expected nil for empty node list")
	}
}

func TestContainsStr(t *testing.T) {
	if !containsStr("hello world", "world") {
		t.Error("expected true for substring match")
	}
	if containsStr("", "world") {
		t.Error("expected false for empty source")
	}
	if !containsStr("hello", "") {
		t.Error("expected true for empty substring in non-empty source")
	}
}
