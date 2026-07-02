package blocks

import (
	"encoding/json"
	"os"
	"path/filepath"
	"testing"

	"flowy/services/mac-daemon/src/foundation"
	"flowy/services/mac-daemon/src/proto"
)

// --- Reverse tests: verify what will NOT happen ---

const toggleTestRunID = "2026-07-02T10-00-00_toggle_test"

func makeToggleTransport() *Transport {
	return &Transport{
		NextSeq: func() int64 { return 1 },
		Send:    func(cmd proto.CommandEnvelope) error { return nil },
		RegisterPending: func(requestID string) chan proto.ResponseEnvelope {
			ch := make(chan proto.ResponseEnvelope, 1)
			ch <- proto.ResponseEnvelope{
				ProtocolVersion: "exp01",
				RequestID:       requestID,
				RunID:           toggleTestRunID,
				Command:         "dump-ui-tree-root",
				Status:          "ok",
				Artifacts: []proto.ArtifactDescriptor{
					{FileName: "tree.json", Kind: "accessibility-tree"},
				},
			}
			return ch
		},
		CancelPending: func(requestID string) {},
		ClientExists:  func(deviceID string) bool { return true },
	}
}

func writeToggleArtifact(nodes []foundation.UiNode) {
	dir := foundation.RunDirFromRunID("", toggleTestRunID)
	_ = os.MkdirAll(dir, 0755)
	data := map[string]any{"packageName": "com.test", "nodes": nodes}
	raw, _ := json.Marshal(data)
	_ = os.WriteFile(filepath.Join(dir, "tree.json"), raw, 0644)
}

func TestDetectToggleState_NoMatchingNodes_ReturnsFalse(t *testing.T) {
	nodes := []foundation.UiNode{
		{Text: "分享", ClassName: "android.widget.Button",
			BoundsInScreen: &foundation.Bounds{Left: 100, Top: 100, Right: 200, Bottom: 200}},
	}
	writeToggleArtifact(nodes)
	tr := makeToggleTransport()

	action := ToggleAction{
		ActionName:   "like",
		ToggledDesc:  "已点赞",
		UntoggledDesc: "点赞",
	}

	state, err := detectToggleStateWithTransport(tr, "", action, 1000)
	if err != nil {
		t.Fatalf("unexpected error: %v", err)
	}
	if state {
		t.Error("expected false (no matching toggled node), got true")
	}
}

func TestDetectToggleState_UntoggledDesc_DoesNotTriggerToggled(t *testing.T) {
	nodes := []foundation.UiNode{
		{ContentDescription: "点赞 5",
			ClassName:          "android.widget.Button",
			BoundsInScreen:     &foundation.Bounds{Left: 100, Top: 100, Right: 200, Bottom: 200}},
	}
	writeToggleArtifact(nodes)
	tr := makeToggleTransport()

	action := ToggleAction{
		ActionName:   "like",
		ToggledDesc:  "已点赞",
		UntoggledDesc: "点赞",
	}

	state, err := detectToggleStateWithTransport(tr, "", action, 1000)
	if err != nil {
		t.Fatalf("unexpected error: %v", err)
	}
	if state {
		t.Error("expected false (untoggled node should not be detected as toggled), got true")
	}
}

func TestTapToggleButton_NoMatchingNodes_ReturnsError(t *testing.T) {
	nodes := []foundation.UiNode{
		{Text: "share", ClassName: "android.widget.Button",
			BoundsInScreen: &foundation.Bounds{Left: 100, Top: 100, Right: 200, Bottom: 200}},
	}
	writeToggleArtifact(nodes)
	tr := makeToggleTransport()

	action := ToggleAction{
		ActionName:   "like",
		ToggledDesc:  "已点赞",
		UntoggledDesc: "点赞",
	}

	err := tapToggleButtonWithTransport(tr, "", action, 1000, BackendAccessibility)
	if err == nil {
		t.Error("expected error when no matching toggle button found, got nil")
	}
}

func TestTapToggleButton_NilBounds_Skipped(t *testing.T) {
	nodes := []foundation.UiNode{
		{ContentDescription: "点赞", ClassName: "android.widget.Button", BoundsInScreen: nil},
	}
	writeToggleArtifact(nodes)
	tr := makeToggleTransport()

	action := ToggleAction{
		ActionName:   "like",
		ToggledDesc:  "已点赞",
		UntoggledDesc: "点赞",
	}

	err := tapToggleButtonWithTransport(tr, "", action, 1000, BackendAccessibility)
	if err == nil {
		t.Error("expected error when all matching nodes have nil bounds, got nil")
	}
}

func TestDetectToggleState_ObserveError_ReturnsError(t *testing.T) {
	tr := &Transport{
		NextSeq: func() int64 { return 1 },
		Send:    func(cmd proto.CommandEnvelope) error { return nil },
		RegisterPending: func(requestID string) chan proto.ResponseEnvelope {
			ch := make(chan proto.ResponseEnvelope, 1)
			ch <- proto.ResponseEnvelope{
				ProtocolVersion: "exp01",
				RequestID:       requestID,
				Command:         "dump-ui-tree-root",
				Status:          "error",
				Error:           &proto.ErrorObject{Code: "DEVICE_OFFLINE", Message: "offline"},
			}
			return ch
		},
		CancelPending: func(requestID string) {},
		ClientExists:  func(deviceID string) bool { return true },
	}

	action := ToggleAction{
		ActionName:   "like",
		ToggledDesc:  "已点赞",
		UntoggledDesc: "点赞",
	}

	_, err := detectToggleStateWithTransport(tr, "", action, 1000)
	if err == nil {
		t.Error("expected error on observe failure, got nil")
	}
}
