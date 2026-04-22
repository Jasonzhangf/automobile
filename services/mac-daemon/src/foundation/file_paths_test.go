package foundation

import "testing"

func TestRunDirUsesDatePartition(t *testing.T) {
	runDir := RunDir("artifacts", "2026-04-21T16:10:04+08:00", "run-1")
	expected := "artifacts/2026-04-21/run-1"
	if runDir != expected {
		t.Fatalf("expected %s, got %s", expected, runDir)
	}
}

func TestRunDirFromRunIDUsesRunIDDate(t *testing.T) {
	runDir := RunDirFromRunID("artifacts", "2026-04-21T16-10-03_ping_boot-check")
	expected := "artifacts/2026-04-21/2026-04-21T16-10-03_ping_boot-check"
	if runDir != expected {
		t.Fatalf("expected %s, got %s", expected, runDir)
	}
}
