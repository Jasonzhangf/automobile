package foundation

import (
	"testing"
	"time"
)

func TestNewRunIDIsValid(t *testing.T) {
	runID := NewRunID(time.Date(2026, 4, 21, 16, 10, 3, 0, time.UTC), "ping", "boot-check")
	if !IsValidRunID(runID) {
		t.Fatalf("expected valid run id, got %s", runID)
	}
}
