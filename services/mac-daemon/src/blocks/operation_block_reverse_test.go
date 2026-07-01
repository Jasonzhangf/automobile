package blocks

import (
	"errors"
	"testing"

	"flowy/services/mac-daemon/src/foundation"
	"flowy/services/mac-daemon/src/proto"
)

// newTestTransportErr creates a transport with configurable send behavior.
// If pendingResp is non-nil, RegisterPending returns a pre-loaded channel.
// If sendErr is non-nil, Send returns that error.
func newTestTransportErr(sendErr error, pendingResp *proto.ResponseEnvelope) *Transport {
	return &Transport{
		NextSeq: func() int64 { return 1 },
		Send:    func(cmd proto.CommandEnvelope) error { return sendErr },
		RegisterPending: func(requestID string) chan proto.ResponseEnvelope {
			ch := make(chan proto.ResponseEnvelope, 1)
			if pendingResp != nil {
				resp := *pendingResp
				resp.RequestID = requestID
				ch <- resp
			}
			return ch
		},
		CancelPending: func(requestID string) {},
		ClientExists:  func(deviceID string) bool { return true },
	}
}

// TAP REVERSE: tap with non-ok status must NOT silently succeed.
func TestTapWithTransport_ResponseNotOk_ReturnsError(t *testing.T) {
	resp := &proto.ResponseEnvelope{
		ProtocolVersion: "exp01",
		Command:         "tap",
		Status:          "error",
		Error:           &proto.ErrorObject{Code: "DEVICE_BUSY", Message: "device busy"},
	}
	tr := newTestTransportErr(nil, resp)
	x, y := 100, 200
	err := tapWithTransport(tr, "", TapTarget{X: &x, Y: &y}, BackendRoot, 1000)
	if err == nil {
		t.Fatal("expected error when response.Status=error, got nil")
	}
}

// TAP REVERSE: missing selector (no coords, no snapshot) must NOT silently tap origin.
func TestTapWithTransport_NoCoordsNoSelector_ReturnsError(t *testing.T) {
	tr := newTestTransportErr(nil, nil)
	_, _, err := resolveTapPoint(tr, "", TapTarget{DescContains: "missing"}, 1000)
	if err == nil {
		t.Fatal("expected error for missing selector, got nil")
	}
}

// SCROLL REVERSE: empty direction must default to "forward" silently inside wrapper
// AND must NOT silently downgrade to "down" or other implicit directions.
func TestScrollWithTransport_EmptyDirection_DefaultsForward(t *testing.T) {
	resp := &proto.ResponseEnvelope{
		ProtocolVersion: "exp01",
		Command:         "scroll",
		Status:          "ok",
	}
	tr := newTestTransportErr(nil, resp)
	x, y := 540, 1200
	if err := scrollWithTransport(tr, "", ScrollTarget{X: &x, Y: &y}, "", BackendRoot, 1000); err != nil {
		t.Fatalf("expected no error, got: %v", err)
	}
}

// SCROLL REVERSE: error response must NOT silently succeed.
func TestScrollWithTransport_ErrorResponse_ReturnsError(t *testing.T) {
	resp := &proto.ResponseEnvelope{
		ProtocolVersion: "exp01",
		Command:         "scroll",
		Status:          "error",
		Error:           &proto.ErrorObject{Code: "SCROLL_FAILED", Message: "no scrollable view"},
	}
	tr := newTestTransportErr(nil, resp)
	x, y := 540, 1200
	if err := scrollWithTransport(tr, "", ScrollTarget{X: &x, Y: &y}, "forward", BackendRoot, 1000); err == nil {
		t.Fatal("expected error for scroll error response, got nil")
	}
}

// BACK REVERSE: error response must NOT silently succeed.
func TestBackBlock_ErrorResponse_ReturnsError(t *testing.T) {
	resp := &proto.ResponseEnvelope{
		ProtocolVersion: "exp01",
		Command:         "press-key",
		Status:          "error",
		Error:           &proto.ErrorObject{Code: "BACK_FAILED", Message: "no key handler"},
	}
	tr := newTestTransportErr(nil, resp)
	if err := backWithTransport(tr, BackendRoot, 1000); err == nil {
		t.Fatal("expected error for back error response, got nil")
	}
}

// INPUT REVERSE: error response must NOT silently succeed.
func TestInputTextBlock_ErrorResponse_ReturnsError(t *testing.T) {
	resp := &proto.ResponseEnvelope{
		ProtocolVersion: "exp01",
		Command:         "input-text",
		Status:          "error",
		Error:           &proto.ErrorObject{Code: "INPUT_FAILED", Message: "no input field"},
	}
	tr := newTestTransportErr(nil, resp)
	if err := inputTextWithTransport(tr, "hello world", BackendRoot, 1000); err == nil {
		t.Fatal("expected error for input error response, got nil")
	}
}

// FIND NODE: empty list must return nil (no random panic or zero node).
func TestFindNodeBySelector_NilNodes_ReturnsNil(t *testing.T) {
	if n := findNodeBySelector(nil, "desc", "text"); n != nil {
		t.Fatal("expected nil for nil node list")
	}
}

// FIND NODE: both desc+text empty means no constraint defined — must return nil
// (never return the first node blindly).
func TestFindNodeBySelector_NoConstraints_ReturnsNil(t *testing.T) {
	nodes := []foundation.UiNode{
		{Text: "anything", ClassName: "android.widget.Button", BoundsInScreen: &foundation.Bounds{Left: 0, Top: 0, Right: 100, Bottom: 50}},
	}
	if n := findNodeBySelector(nodes, "", ""); n != nil {
		t.Fatal("expected nil when no constraint provided, got non-nil")
	}
}

// FIND NODE: must NOT match arbitrary unrelated text.
func TestFindNodeBySelector_UnrelatedText_NoMatch(t *testing.T) {
	nodes := []foundation.UiNode{
		{Text: "unrelated", ClassName: "android.widget.Button", BoundsInScreen: &foundation.Bounds{Left: 0, Top: 0, Right: 100, Bottom: 50}},
	}
	if n := findNodeBySelector(nodes, "", "expected_text"); n != nil {
		t.Fatal("expected nil when no node matches text, got non-nil")
	}
}

// FIND NODE: must match even when only desc is non-empty.
func TestFindNodeBySelector_DescOnly_Match(t *testing.T) {
	nodes := []foundation.UiNode{
		{ContentDescription: "搜索", ClassName: "android.widget.Button", BoundsInScreen: &foundation.Bounds{Left: 0, Top: 0, Right: 100, Bottom: 50}},
	}
	if n := findNodeBySelector(nodes, "搜索", ""); n == nil {
		t.Fatal("expected match by desc only, got nil")
	}
}

// SEND FAILURE: tap with Send error must NOT swallow the error.
func TestTapWithTransport_SendError_Propagates(t *testing.T) {
	tr := newTestTransportErr(errors.New("ws disconnected"), nil)
	x, y := 100, 200
	if err := tapWithTransport(tr, "", TapTarget{X: &x, Y: &y}, BackendRoot, 1000); err == nil {
		t.Fatal("expected error when Send fails, got nil")
	}
}
