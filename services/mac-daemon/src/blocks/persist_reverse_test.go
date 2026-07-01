package blocks

import (
	"path/filepath"
	"strings"
	"testing"

	"flowy/services/mac-daemon/src/foundation"
	"flowy/services/mac-daemon/src/proto"
)

// PERSIST REVERSE: empty LogEntry slice must still produce valid file (no panic).
func TestPersistLogsArtifact_EmptyEntries_ProducesHeaderlessFile(t *testing.T) {
	dir := t.TempDir()
	if err := PersistLogsArtifact(dir, []proto.LogEntry{}); err != nil {
		t.Fatalf("expected no error on empty entries, got: %v", err)
	}
	data, err := readFileOS(filepath.Join(dir, "logs.txt"))
	if err != nil {
		t.Fatalf("expected logs.txt file, got: %v", err)
	}
	if len(data) != 0 {
		t.Fatalf("expected empty content, got %d bytes", len(data))
	}
}

// PERSIST REVERSE: nil entries must NOT panic.
func TestPersistLogsArtifact_NilEntries_NoPanic(t *testing.T) {
	dir := t.TempDir()
	defer func() {
		if r := recover(); r != nil {
			t.Fatalf("nil entries must NOT panic, got: %v", r)
		}
	}()
	if err := PersistLogsArtifact(dir, nil); err != nil {
		t.Fatalf("expected no error on nil entries, got: %v", err)
	}
}

// PERSIST REVERSE: missing fileName must be rejected.
func TestPersistUploadedArtifact_MissingFileName_ReturnsError(t *testing.T) {
	dir := t.TempDir()
	meta := proto.ArtifactUploadMeta{Kind: "screenshot", ContentType: "image/png"}
	if _, err := PersistUploadedArtifact(dir, meta, []byte("data")); err == nil {
		t.Fatal("expected error for missing fileName, got nil")
	}
}

// PERSIST REVERSE: missing kind must be rejected.
func TestPersistUploadedArtifact_MissingKind_ReturnsError(t *testing.T) {
	dir := t.TempDir()
	meta := proto.ArtifactUploadMeta{FileName: "x.png", ContentType: "image/png"}
	if _, err := PersistUploadedArtifact(dir, meta, []byte("data")); err == nil {
		t.Fatal("expected error for missing kind, got nil")
	}
}

// PERSIST REVERSE: descriptor must round-trip the content faithfully.
func TestPersistUploadedArtifact_DescriptorFaithful(t *testing.T) {
	dir := t.TempDir()
	meta := proto.ArtifactUploadMeta{
		FileName:    "deepseek_v4.png",
		Kind:        "screenshot",
		ContentType: "image/png",
	}
	content := []byte("png-bytes")
	desc, err := PersistUploadedArtifact(dir, meta, content)
	if err != nil {
		t.Fatalf("unexpected error: %v", err)
	}
	if desc.FileName != "deepseek_v4.png" {
		t.Errorf("descriptor.FileName mismatch: %q", desc.FileName)
	}
	if desc.ContentType != "image/png" {
		t.Errorf("descriptor.ContentType mismatch: %q", desc.ContentType)
	}
	if desc.SizeBytes != int64(len(content)) {
		t.Errorf("descriptor.SizeBytes mismatch: %d", desc.SizeBytes)
	}
	if desc.SHA256 == "" {
		t.Error("SHA256 must be non-empty for non-empty content")
	}
	if len(desc.SHA256) != 64 {
		t.Errorf("SHA256 must be 64 hex chars, got %d", len(desc.SHA256))
	}
	expected := foundation.SHA256Hex([]byte("png-bytes"))
	if !strings.EqualFold(desc.SHA256, expected) {
		t.Errorf("SHA256 must equal re-hashed content, got %q want %q", desc.SHA256, expected)
	}
}
