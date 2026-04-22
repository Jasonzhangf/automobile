package flows

import (
	"encoding/json"
	"os"
	"path/filepath"
	"testing"

	"flowy/services/mac-daemon/src/proto"
)

func TestFinalizeRunIncludesArtifactFilesInManifest(t *testing.T) {
	artifactRoot := t.TempDir()
	response := proto.ResponseEnvelope{
		ProtocolVersion: "exp01",
		RequestID:       "req_20260422_101000_0001",
		RunID:           "2026-04-22T10-10-00_capture-screenshot_home-screen",
		Command:         "capture-screenshot",
		Status:          "ok",
		StartedAt:       "2026-04-22T10:10:01+08:00",
		FinishedAt:      "2026-04-22T10:10:02+08:00",
		Device:          proto.DeviceMeta{DeviceID: "android-local-01"},
		App:             proto.AppMeta{PackageName: "com.flowy.explore", RuntimeVersion: "0.1.0007"},
		Artifacts: []proto.ArtifactDescriptor{
			{Kind: "screenshot", FileName: "screenshot.png", ContentType: "image/png"},
			{Kind: "page-context", FileName: "page-context.json", ContentType: "application/json"},
		},
		Message: "captured",
	}

	runDir, err := FinalizeRun(artifactRoot, response)
	if err != nil {
		t.Fatalf("finalize failed: %v", err)
	}

	manifestPath := filepath.Join(runDir, "manifest.json")
	data, err := os.ReadFile(manifestPath)
	if err != nil {
		t.Fatalf("read manifest failed: %v", err)
	}
	var manifest proto.RunManifest
	if err := json.Unmarshal(data, &manifest); err != nil {
		t.Fatalf("decode manifest failed: %v", err)
	}
	for _, name := range []string{"response.json", "screenshot.png", "page-context.json"} {
		if !contains(manifest.Files, name) {
			t.Fatalf("expected manifest to contain %s, got %#v", name, manifest.Files)
		}
	}
}

func contains(values []string, target string) bool {
	for _, value := range values {
		if value == target {
			return true
		}
	}
	return false
}
