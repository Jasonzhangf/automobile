package foundation

import (
	"testing"
	"time"
)

func TestRandomDuration_Bounds(t *testing.T) {
	minMs := 100
	maxMs := 500
	for i := 0; i < 200; i++ {
		d := RandomDuration(minMs, maxMs)
		if d < time.Duration(minMs)*time.Millisecond || d > time.Duration(maxMs)*time.Millisecond {
			t.Fatalf("out of bounds: %v (expected [%d, %d] ms)", d, minMs, maxMs)
		}
	}
}

func TestRandomDuration_SameMinMax(t *testing.T) {
	d := RandomDuration(42, 42)
	if d != 42*time.Millisecond {
		t.Fatalf("expected 42ms, got %v", d)
	}
}

func TestRandomDuration_MinGreaterThanMax(t *testing.T) {
	d := RandomDuration(500, 100)
	// max < min → clamp max to min (=500) → always returns 500ms
	if d != 500*time.Millisecond {
		t.Fatalf("expected 500ms, got %v", d)
	}
}

func TestRandomDuration_NegativeMin(t *testing.T) {
	d := RandomDuration(-50, 100)
	// negative min → clamp to 0, result should be in [0, 100]
	if d < 0 || d > 100*time.Millisecond {
		t.Fatalf("out of bounds: %v", d)
	}
}

func TestRandomDuration_Variability(t *testing.T) {
	// Over 100 calls, expect at least 2 distinct values.
	seen := make(map[time.Duration]bool)
	for i := 0; i < 100; i++ {
		seen[RandomDuration(800, 2000)] = true
	}
	if len(seen) < 2 {
		t.Fatalf("expected variability, got only %d distinct values", len(seen))
	}
}

func TestOperationDelay_Range(t *testing.T) {
	for i := 0; i < 100; i++ {
		d := OperationDelay()
		if d < 800*time.Millisecond || d > 2000*time.Millisecond {
			t.Fatalf("OperationDelay out of bounds: %v", d)
		}
	}
}

func TestScrollDelay_Range(t *testing.T) {
	for i := 0; i < 100; i++ {
		d := ScrollDelay()
		if d < 800*time.Millisecond || d > 1500*time.Millisecond {
			t.Fatalf("ScrollDelay out of bounds: %v", d)
		}
	}
}

func TestAnchorPollDelay_Range(t *testing.T) {
	for i := 0; i < 100; i++ {
		d := AnchorPollDelay()
		if d < 800*time.Millisecond || d > 1500*time.Millisecond {
			t.Fatalf("AnchorPollDelay out of bounds: %v", d)
		}
	}
}
