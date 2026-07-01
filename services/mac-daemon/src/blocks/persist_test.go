package blocks

import (
	"path/filepath"
	"testing"

	"flowy/services/mac-daemon/src/proto"
)

func TestPersistManifest(t *testing.T) {
	dir := t.TempDir()
	err := PersistManifest(dir, map[string]string{"key": "value"})
	if err != nil {
		t.Fatalf("expected no error, got: %v", err)
	}
	path := filepath.Join(dir, "manifest.json")
	if _, err := readFile(path); err != nil {
		t.Errorf("expected manifest file, got error: %v", err)
	}
}

func TestPersistResponse(t *testing.T) {
	dir := t.TempDir()
	resp := proto.ResponseEnvelope{
		ProtocolVersion: "exp01",
		RequestID:       "req-1-abc",
		Command:         "ping",
		Status:          "ok",
	}
	err := PersistResponse(dir, resp)
	if err != nil {
		t.Fatalf("expected no error, got: %v", err)
	}
	path := filepath.Join(dir, "response.json")
	if _, err := readFile(path); err != nil {
		t.Errorf("expected response file, got error: %v", err)
	}
}

func TestPersistLogsArtifact_Empty(t *testing.T) {
	dir := t.TempDir()
	err := PersistLogsArtifact(dir, nil)
	if err != nil {
		t.Fatalf("expected no error for nil entries, got: %v", err)
	}
}

func TestPersistLogsArtifact_WithEntries(t *testing.T) {
	dir := t.TempDir()
	entries := []proto.LogEntry{
		{TS: "2026-07-01T00:00:00Z", Level: "info", Event: "test", Message: "hello"},
	}
	err := PersistLogsArtifact(dir, entries)
	if err != nil {
		t.Fatalf("expected no error, got: %v", err)
	}
}

func TestPersistUploadedArtifact(t *testing.T) {
	dir := t.TempDir()
	meta := proto.ArtifactUploadMeta{
		ProtocolVersion: "exp01",
		RequestID:       "req-1-abc",
		RunID:           "run-xyz",
		DeviceID:        "OP64DDL1",
		Kind:            "screenshot",
		FileName:        "test.png",
		ContentType:     "image/png",
	}
	content := []byte("fake png content")
	desc, err := PersistUploadedArtifact(dir, meta, content)
	if err != nil {
		t.Fatalf("expected no error, got: %v", err)
	}
	if desc.FileName != "test.png" {
		t.Errorf("expected fileName=test.png, got %q", desc.FileName)
	}
}

func readFile(path string) ([]byte, error) {
	return readFileImpl(path)
}

func readFileImpl(path string) ([]byte, error) {
	return readFileOS(path)
}
