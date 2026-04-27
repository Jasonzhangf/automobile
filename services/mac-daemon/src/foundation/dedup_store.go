package foundation

import (
	"bufio"
	"encoding/json"
	"os"
	"sync"
	"time"
)

// DedupEntry represents one processed item in the dedup store.
type DedupEntry struct {
	ItemID      string `json:"item_id"`
	Title       string `json:"title,omitempty"`
	URL         string `json:"url,omitempty"`
	ProcessedAt string `json:"processed_at"`
}

// DedupStore is a two-layer dedup: in-memory set + JSONL file on disk.
type DedupStore struct {
	mu    sync.RWMutex
	path  string
	items map[string]DedupEntry
}

// LoadDedupStore reads an existing JSONL dedup file into memory.
// If the file does not exist, returns an empty store ready for use.
func LoadDedupStore(path string) (*DedupStore, error) {
	s := &DedupStore{
		path:  path,
		items: make(map[string]DedupEntry),
	}
	f, err := os.Open(path)
	if err != nil {
		if os.IsNotExist(err) {
			return s, nil
		}
		return nil, err
	}
	defer f.Close()

	scanner := bufio.NewScanner(f)
	scanner.Buffer(make([]byte, 0, 64*1024), 64*1024)
	for scanner.Scan() {
		line := scanner.Bytes()
		if len(line) == 0 {
			continue
		}
		var entry DedupEntry
		if err := json.Unmarshal(line, &entry); err != nil {
			continue
		}
		if entry.ItemID != "" {
			s.items[entry.ItemID] = entry
		}
	}
	return s, scanner.Err()
}

// Contains returns true if itemID has already been processed.
func (s *DedupStore) Contains(itemID string) bool {
	s.mu.RLock()
	defer s.mu.RUnlock()
	_, ok := s.items[itemID]
	return ok
}

// Count returns how many items are in the dedup store.
func (s *DedupStore) Count() int {
	s.mu.RLock()
	defer s.mu.RUnlock()
	return len(s.items)
}

// Append adds a new item to the dedup store and persists it to the JSONL file.
// Returns false if the item already existed (duplicate).
func (s *DedupStore) Append(itemID, title, url string) (bool, error) {
	s.mu.Lock()
	defer s.mu.Unlock()

	if _, exists := s.items[itemID]; exists {
		return false, nil
	}

	entry := DedupEntry{
		ItemID:      itemID,
		Title:       title,
		URL:         url,
		ProcessedAt: time.Now().UTC().Format(time.RFC3339),
	}

	line, err := json.Marshal(entry)
	if err != nil {
		return false, err
	}
	f, err := os.OpenFile(s.path, os.O_APPEND|os.O_CREATE|os.O_WRONLY, 0644)
	if err != nil {
		return false, err
	}
	_, err = f.Write(append(line, '\n'))
	f.Close()
	if err != nil {
		return false, err
	}

	s.items[itemID] = entry
	return true, nil
}

// AllIDs returns a copy of all item IDs currently in the store.
func (s *DedupStore) AllIDs() []string {
	s.mu.RLock()
	defer s.mu.RUnlock()
	ids := make([]string, 0, len(s.items))
	for id := range s.items {
		ids = append(ids, id)
	}
	return ids
}

