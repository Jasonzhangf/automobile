package foundation

import (
	"os"
	"path/filepath"
	"testing"
)

func TestLoadDedupStore_FileNotExist(t *testing.T) {
	path := filepath.Join(t.TempDir(), "nonexistent.jsonl")
	store, err := LoadDedupStore(path)
	if err != nil {
		t.Fatalf("unexpected error: %v", err)
	}
	if store.Count() != 0 {
		t.Fatalf("expected 0 items, got %d", store.Count())
	}
}

func TestDedupStore_AppendAndContains(t *testing.T) {
	path := filepath.Join(t.TempDir(), "dedup.jsonl")
	store, err := LoadDedupStore(path)
	if err != nil {
		t.Fatalf("load error: %v", err)
	}

	ok, err := store.Append("id-001", "Test Post Title", "https://example.com/1")
	if err != nil {
		t.Fatalf("append error: %v", err)
	}
	if !ok {
		t.Fatal("expected Append to return true for new item")
	}
	if !store.Contains("id-001") {
		t.Fatal("expected Contains to return true after Append")
	}
	if store.Count() != 1 {
		t.Fatalf("expected 1 item, got %d", store.Count())
	}

	// Duplicate append should return false.
	ok, err = store.Append("id-001", "Test Post Title", "https://example.com/1")
	if err != nil {
		t.Fatalf("append error: %v", err)
	}
	if ok {
		t.Fatal("expected Append to return false for duplicate")
	}
	if store.Count() != 1 {
		t.Fatalf("expected still 1 item, got %d", store.Count())
	}
}

func TestDedupStore_Persistence(t *testing.T) {
	path := filepath.Join(t.TempDir(), "dedup.jsonl")

	// Write items with first store instance.
 store1, err := LoadDedupStore(path)
	if err != nil {
		t.Fatalf("load error: %v", err)
	}
	store1.Append("id-A", "Post A", "url-a")
	store1.Append("id-B", "Post B", "url-b")

	// Reload from disk with a new store instance.
 store2, err := LoadDedupStore(path)
	if err != nil {
		t.Fatalf("reload error: %v", err)
	}
	if !store2.Contains("id-A") || !store2.Contains("id-B") {
		t.Fatal("reloaded store should contain both items")
	}
	if store2.Count() != 2 {
		t.Fatalf("expected 2 items after reload, got %d", store2.Count())
	}
}

func TestDedupStore_MalformedLines(t *testing.T) {
	dir := t.TempDir()
	path := filepath.Join(dir, "dedup.jsonl")
	// Write a file with one good line and one bad line.
	content := "{\"item_id\":\"good\",\"title\":\"ok\"}\nnot json at all\n"
	os.WriteFile(path, []byte(content), 0644)

	store, err := LoadDedupStore(path)
	if err != nil {
		t.Fatalf("load error: %v", err)
	}
	if !store.Contains("good") {
		t.Fatal("should contain the good item")
	}
	if store.Count() != 1 {
		t.Fatalf("expected 1 (malformed skipped), got %d", store.Count())
	}
}

func TestDedupStore_AllIDs(t *testing.T) {
	path := filepath.Join(t.TempDir(), "dedup.jsonl")
	store, _ := LoadDedupStore(path)
	store.Append("x1", "t1", "")
	store.Append("x2", "t2", "")
	ids := store.AllIDs()
	if len(ids) != 2 {
		t.Fatalf("expected 2 ids, got %d", len(ids))
	}
	idSet := make(map[string]bool)
	for _, id := range ids {
		idSet[id] = true
	}
	if !idSet["x1"] || !idSet["x2"] {
		t.Fatalf("expected x1 and x2 in AllIDs, got %v", ids)
	}
}

func TestDedupStore_FileCreatedOnAppend(t *testing.T) {
	path := filepath.Join(t.TempDir(), "sub", "dedup.jsonl")
	// Ensure parent dir exists (simulating real use where output dir is created first).
	os.MkdirAll(filepath.Dir(path), 0755)
	store, _ := LoadDedupStore(path)
	store.Append("new-id", "title", "")

	// Verify file was created and has one line.
	data, err := os.ReadFile(path)
	if err != nil {
		t.Fatalf("file not created: %v", err)
	}
	if len(data) == 0 {
		t.Fatal("file is empty")
	}
}
