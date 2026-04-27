package blocks

import (
	"fmt"
	"time"

	"flowy/services/mac-daemon/src/foundation"
	"flowy/services/mac-daemon/src/state"
)

// AnchorConfig configures the bounded-poll anchor evaluation.
type AnchorConfig struct {
	// MaxRetries is the maximum number of observe+evaluate cycles.
	MaxRetries int
	// PollDelayFn returns a random delay between polls.
	// Defaults to foundation.AnchorPollDelay if nil.
	PollDelayFn func() time.Duration
	// ObserveTimeoutMs is the timeout for each ObservePage call.
	ObserveTimeoutMs int
}

// DefaultAnchorConfig returns sensible defaults for anchor polling.
func DefaultAnchorConfig() AnchorConfig {
	return AnchorConfig{
		MaxRetries:       3,
		PollDelayFn:      foundation.AnchorPollDelay,
		ObserveTimeoutMs: 8000,
	}
}

// AnchorBlockResult is the outcome of an anchor evaluation block.
type AnchorBlockResult struct {
	Matched      bool                   `json:"matched"`
	AttemptCount int                    `json:"attemptCount"`
	LastResult   foundation.AnchorResult `json:"lastResult"`
	Nodes        []foundation.UiNode    `json:"-"` // available for downstream blocks
	PackageName  string                 `json:"packageName"`
}

// AnchorBlock evaluates an AnchorSpec with bounded retry.
// Each attempt: ObservePage → EvaluateAnchor → if not matched, sleep + retry.
func AnchorBlock(
	session *state.ClientSession,
	app *state.AppState,
	artifactRoot string,
	spec foundation.AnchorSpec,
	cfg AnchorConfig,
) (*AnchorBlockResult, error) {
	if cfg.MaxRetries <= 0 {
		cfg.MaxRetries = 3
	}
	if cfg.PollDelayFn == nil {
		cfg.PollDelayFn = foundation.AnchorPollDelay
	}
	if cfg.ObserveTimeoutMs <= 0 {
		cfg.ObserveTimeoutMs = 8000
	}

	var lastResult foundation.AnchorResult
	for attempt := 1; attempt <= cfg.MaxRetries; attempt++ {
		obs, err := ObservePage(session, app, artifactRoot, cfg.ObserveTimeoutMs)
		if err != nil {
			lastResult = foundation.AnchorResult{
				Matched: false,
				Reasons: []string{fmt.Sprintf("observe failed on attempt %d: %v", attempt, err)},
			}
			// observe error → sleep then retry
			if attempt < cfg.MaxRetries {
				time.Sleep(cfg.PollDelayFn())
			}
			continue
		}

		lastResult = foundation.EvaluateAnchor(obs.Nodes, spec)
		if lastResult.Matched {
			return &AnchorBlockResult{
				Matched:      true,
				AttemptCount: attempt,
				LastResult:   lastResult,
				Nodes:        obs.Nodes,
				PackageName:  obs.PackageName,
			}, nil
		}

		// not matched → sleep then retry (unless last attempt)
		if attempt < cfg.MaxRetries {
			time.Sleep(cfg.PollDelayFn())
		}
	}

	return &AnchorBlockResult{
		Matched:      false,
		AttemptCount: cfg.MaxRetries,
		LastResult:   lastResult,
	}, nil
}
