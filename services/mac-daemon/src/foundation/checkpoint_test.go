package foundation

import (
	"os"
	"path/filepath"
	"testing"
)

func TestWriteAndReadCheckpoint(t *testing.T) {
	dir := t.TempDir()
	cp := &Checkpoint{
		RunID:        "test-run-001",
		CurrentState: "DETAIL_TASK",
		EntryContext: EntryContext{
			Mode:    "search",
			Keyword: "deepseek v4",
			App:     "xhs",
		},
		ListPosition: ListPosition{
			ScrollIndex:    3,
			VisibleItemIDs: []string{"id1", "id2", "id3"},
		},
		CurrentItemID: "abc123",
		DetailProgress: DetailProgress{
			Snapshot:           "done",
			Interact:           "pending",
			Comments:           "in_progress",
			CommentScrollCount: 7,
		},
		CompletedIDs:   []string{"id0", "id1"},
		CompletedCount: 2,
		TargetCount:    20,
		ErrorCount:     1,
	}

	if err := WriteCheckpoint(dir, cp); err != nil {
		t.Fatalf("WriteCheckpoint failed: %v", err)
	}

	loaded, err := ReadCheckpoint(dir)
	if err != nil {
		t.Fatalf("ReadCheckpoint failed: %v", err)
	}
	if loaded == nil {
		t.Fatal("expected non-nil checkpoint")
	}
	if loaded.SchemaVersion != checkpointSchemaVersion {
		t.Fatalf("expected schema %s, got %s", checkpointSchemaVersion, loaded.SchemaVersion)
	}
	if loaded.RunID != "test-run-001" {
		t.Fatalf("expected runId test-run-001, got %s", loaded.RunID)
	}
	if loaded.CurrentState != "DETAIL_TASK" {
		t.Fatalf("expected state DETAIL_TASK, got %s", loaded.CurrentState)
	}
	if loaded.EntryContext.Keyword != "deepseek v4" {
		t.Fatalf("expected keyword deepseek v4, got %s", loaded.EntryContext.Keyword)
	}
	if loaded.ListPosition.ScrollIndex != 3 {
		t.Fatalf("expected scrollIndex 3, got %d", loaded.ListPosition.ScrollIndex)
	}
	if len(loaded.CompletedIDs) != 2 {
		t.Fatalf("expected 2 completed IDs, got %d", len(loaded.CompletedIDs))
	}
	if loaded.DetailProgress.CommentScrollCount != 7 {
		t.Fatalf("expected commentScrollCount 7, got %d", loaded.DetailProgress.CommentScrollCount)
	}
	if loaded.Timestamp == "" {
		t.Fatal("expected non-empty timestamp")
	}
}

func TestReadCheckpoint_NotFound(t *testing.T) {
	dir := t.TempDir()
	cp, err := ReadCheckpoint(dir)
	if err != nil {
		t.Fatalf("expected no error for missing checkpoint, got %v", err)
	}
	if cp != nil {
		t.Fatal("expected nil checkpoint for missing file")
	}
}

func TestDeleteCheckpoint(t *testing.T) {
	dir := t.TempDir()
	cp := &Checkpoint{RunID: "del-test", CurrentState: "INIT"}
	if err := WriteCheckpoint(dir, cp); err != nil {
		t.Fatalf("write failed: %v", err)
	}
	if !CheckpointExists(dir) {
		t.Fatal("expected checkpoint to exist after write")
	}
	if err := DeleteCheckpoint(dir); err != nil {
		t.Fatalf("delete failed: %v", err)
	}
	if CheckpointExists(dir) {
		t.Fatal("expected checkpoint to not exist after delete")
	}
	// Delete again should not error
	if err := DeleteCheckpoint(dir); err != nil {
		t.Fatalf("double delete should not error, got %v", err)
	}
}

func TestCheckpointExists(t *testing.T) {
	dir := t.TempDir()
	if CheckpointExists(dir) {
		t.Fatal("expected no checkpoint in empty dir")
	}
	cp := &Checkpoint{RunID: "exists-test", CurrentState: "PICK_NEXT"}
	if err := WriteCheckpoint(dir, cp); err != nil {
		t.Fatalf("write failed: %v", err)
	}
	if !CheckpointExists(dir) {
		t.Fatal("expected checkpoint to exist")
	}
}

func TestWriteCheckpoint_CreatesDirectory(t *testing.T) {
	dir := filepath.Join(t.TempDir(), "nested", "path")
	cp := &Checkpoint{RunID: "mkdir-test", CurrentState: "INIT"}
	if err := WriteCheckpoint(dir, cp); err != nil {
		t.Fatalf("WriteCheckpoint should create dirs, got %v", err)
	}
	data, err := os.ReadFile(filepath.Join(dir, checkpointFileName))
	if err != nil {
		t.Fatalf("read failed: %v", err)
	}
	if len(data) == 0 {
		t.Fatal("expected non-empty checkpoint file")
	}
}

func TestReadCheckpoint_InvalidJSON(t *testing.T) {
	dir := t.TempDir()
	path := filepath.Join(dir, checkpointFileName)
	if err := os.WriteFile(path, []byte("not json"), 0o644); err != nil {
		t.Fatalf("write failed: %v", err)
	}
	_, err := ReadCheckpoint(dir)
	if err == nil {
		t.Fatal("expected error for invalid JSON")
	}
}

func TestCheckpointRoundTrip_EmptyFields(t *testing.T) {
	dir := t.TempDir()
	cp := &Checkpoint{RunID: "minimal", CurrentState: "DONE"}
	if err := WriteCheckpoint(dir, cp); err != nil {
		t.Fatalf("write failed: %v", err)
	}
	loaded, err := ReadCheckpoint(dir)
	if err != nil {
		t.Fatalf("read failed: %v", err)
	}
	if loaded.CompletedCount != 0 {
		t.Fatalf("expected 0 completed, got %d", loaded.CompletedCount)
	}
	if loaded.EntryContext.Mode != "" {
		t.Fatalf("expected empty mode, got %s", loaded.EntryContext.Mode)
	}
}
