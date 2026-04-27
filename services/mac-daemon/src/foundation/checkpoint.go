package foundation

import (
	"encoding/json"
	"os"
	"path/filepath"
	"time"
)

// Checkpoint is the serializable state for workflow resume.
type Checkpoint struct {
	SchemaVersion  string          `json:"schemaVersion"`
	RunID          string          `json:"runId"`
	Timestamp      string          `json:"timestamp"`
	CurrentState   string          `json:"currentState"`
	EntryContext   EntryContext    `json:"entryContext"`
	ListPosition   ListPosition    `json:"listPosition"`
	CurrentItemID  string          `json:"currentItemId"`
	DetailProgress DetailProgress  `json:"detailProgress"`
	CompletedIDs   []string        `json:"completedItemIds"`
	CompletedCount int             `json:"completedCount"`
	TargetCount    int             `json:"targetCount"`
	ErrorCount     int             `json:"errorCount"`
}

type EntryContext struct {
	Mode    string `json:"mode"`
	Keyword string `json:"keyword,omitempty"`
	App     string `json:"app"`
}

type ListPosition struct {
	ScrollIndex     int      `json:"scrollIndex"`
	VisibleItemIDs  []string `json:"visibleItemIds"`
}

type DetailProgress struct {
	Snapshot            string `json:"snapshot"`
	Interact            string `json:"interact"`
	Comments            string `json:"comments"`
	CommentScrollCount  int    `json:"commentScrollCount"`
	PartialCommentsFile string `json:"partialCommentsFile,omitempty"`
}

const checkpointSchemaVersion = "flowy-checkpoint-v1"
const checkpointFileName = "checkpoint.json"

// WriteCheckpoint serializes and persists a checkpoint to disk.
func WriteCheckpoint(dir string, cp *Checkpoint) error {
	cp.SchemaVersion = checkpointSchemaVersion
	cp.Timestamp = time.Now().Format(time.RFC3339)
	if err := os.MkdirAll(dir, 0o755); err != nil {
		return err
	}
	data, err := json.MarshalIndent(cp, "", "  ")
	if err != nil {
		return err
	}
	return os.WriteFile(filepath.Join(dir, checkpointFileName), data, 0o644)
}

// ReadCheckpoint loads a checkpoint from disk if it exists.
// Returns nil, nil if no checkpoint file is found.
func ReadCheckpoint(dir string) (*Checkpoint, error) {
	path := filepath.Join(dir, checkpointFileName)
	data, err := os.ReadFile(path)
	if err != nil {
		if os.IsNotExist(err) {
			return nil, nil
		}
		return nil, err
	}
	var cp Checkpoint
	if err := json.Unmarshal(data, &cp); err != nil {
		return nil, err
	}
	return &cp, nil
}

// DeleteCheckpoint removes the checkpoint file if it exists.
func DeleteCheckpoint(dir string) error {
	path := filepath.Join(dir, checkpointFileName)
	err := os.Remove(path)
	if err != nil && os.IsNotExist(err) {
		return nil
	}
	return err
}

// CheckpointExists returns true if a checkpoint file is present.
func CheckpointExists(dir string) bool {
	path := filepath.Join(dir, checkpointFileName)
	_, err := os.Stat(path)
	return err == nil
}
