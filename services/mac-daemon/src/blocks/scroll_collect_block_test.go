package blocks

import (
	"encoding/json"
	"os"
	"path/filepath"
	"testing"

	"flowy/services/mac-daemon/src/foundation"
	"flowy/services/mac-daemon/src/proto"
)

// --- helpers ---

// Use date-prefixed runID so RunDirFromRunID resolves correctly.
const testRunID = "2026-07-02T10-00-00_scroll_test"

func writeScrollTreeArtifact(t *testing.T, nodes []foundation.UiNode) {
	t.Helper()
	dir := foundation.RunDirFromRunID("", testRunID)
	_ = os.MkdirAll(dir, 0755)
	data := map[string]any{
		"packageName": "com.test",
		"nodes":       nodes,
	}
	raw, _ := json.Marshal(data)
	_ = os.WriteFile(filepath.Join(dir, "tree.json"), raw, 0644)
}

func makeSimpleScrollTransport() *Transport {
	return &Transport{
		NextSeq: func() int64 { return 1 },
		Send:    func(cmd proto.CommandEnvelope) error { return nil },
		RegisterPending: func(requestID string) chan proto.ResponseEnvelope {
			ch := make(chan proto.ResponseEnvelope, 1)
			ch <- proto.ResponseEnvelope{
				ProtocolVersion: "exp01",
				RequestID:       requestID,
				RunID:           testRunID,
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

// --- tests ---

func TestDefaultScrollCollectConfig(t *testing.T) {
	cfg := DefaultScrollCollectConfig()
	if cfg.MaxScrolls != 30 {
		t.Errorf("expected MaxScrolls=30, got %d", cfg.MaxScrolls)
	}
	if cfg.Backend != "root" {
		t.Errorf("expected Backend='root', got %q", cfg.Backend)
	}
	if cfg.ScrollDelayMs != 800 {
		t.Errorf("expected ScrollDelayMs=800, got %d", cfg.ScrollDelayMs)
	}
}

func TestScrollCollectBlock_ZeroNodes_StillCompletes(t *testing.T) {
	tr := makeSimpleScrollTransport()
	writeScrollTreeArtifact(t, []foundation.UiNode{})

	cfg := ScrollCollectConfig{MaxScrolls: 2, ScrollDelayMs: 10, ScrollJitterMs: 10, TimeoutMs: 1000}
	result, err := scrollCollectBlockWithTransport(tr, "", cfg)
	if err != nil {
		t.Fatalf("expected no error, got: %v", err)
	}
	if result == nil {
		t.Fatal("expected non-nil result")
	}
	if result.ScrollsDone < 1 {
		t.Errorf("expected at least 1 scroll round, got %d", result.ScrollsDone)
	}
}

func TestScrollCollectBlock_DuplicatesSkipped(t *testing.T) {
	tr := makeSimpleScrollTransport()
	writeScrollTreeArtifact(t, []foundation.UiNode{
		{Text: "hello", ClassName: "android.widget.TextView"},
		{Text: "hello", ClassName: "android.widget.TextView"},
	})

	cfg := ScrollCollectConfig{MaxScrolls: 3, ScrollDelayMs: 10, ScrollJitterMs: 10, TimeoutMs: 1000}
	result, err := scrollCollectBlockWithTransport(tr, "", cfg)
	if err != nil {
		t.Fatalf("expected no error, got: %v", err)
	}
	seen := map[string]bool{}
	for _, txt := range result.AllTexts {
		if seen[txt] {
			t.Errorf("duplicate text in AllTexts: %q", txt)
		}
		seen[txt] = true
	}
}

func TestScrollCollectBlock_NoComments_ExtractsTexts(t *testing.T) {
	tr := makeSimpleScrollTransport()
	writeScrollTreeArtifact(t, []foundation.UiNode{
		{Text: "comment A", ClassName: "android.widget.TextView"},
		{Text: "comment B", ClassName: "android.widget.TextView"},
	})

	cfg := ScrollCollectConfig{MaxScrolls: 1, ScrollDelayMs: 10, ScrollJitterMs: 10, TimeoutMs: 1000}
	result, err := scrollCollectBlockWithTransport(tr, "", cfg)
	if err != nil {
		t.Fatalf("expected no error, got: %v", err)
	}
	if len(result.AllTexts) < 2 {
		t.Errorf("expected at least 2 texts, got %d", len(result.AllTexts))
	}
}
