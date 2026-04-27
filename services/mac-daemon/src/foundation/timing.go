package foundation

import (
	"math/rand"
	"time"
)

// RandomDuration returns a random duration in [minMs, maxMs] milliseconds.
// Used for all operation intervals to avoid detection by fixed-timing heuristics.
func RandomDuration(minMs, maxMs int) time.Duration {
	if minMs < 0 {
		minMs = 0
	}
	if maxMs < minMs {
		maxMs = minMs
	}
	ms := minMs
	if maxMs > minMs {
		ms = minMs + rand.Intn(maxMs-minMs+1)
	}
	return time.Duration(ms) * time.Millisecond
}

// OperationDelay returns a random delay suitable between user-like operations.
// Default range: [800, 2000]ms per the collection-workflow-skeleton spec.
func OperationDelay() time.Duration {
	return RandomDuration(800, 2000)
}

// ScrollDelay returns a random delay suitable between scroll operations.
// Default range: [800, 1500]ms.
func ScrollDelay() time.Duration {
	return RandomDuration(800, 1500)
}

// AnchorPollDelay returns a random delay for anchor polling.
// Default range: [800, 1500]ms.
func AnchorPollDelay() time.Duration {
	return RandomDuration(800, 1500)
}
