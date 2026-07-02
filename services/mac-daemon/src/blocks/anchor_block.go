package blocks

import (
	"fmt"
	"time"

	"flowy/services/mac-daemon/src/foundation"
	"flowy/services/mac-daemon/src/state"
)

// AnchorConfig controls bounded anchor poll behavior.
type AnchorConfig struct {
	MaxRetries       int
	PollDelayFn      func() time.Duration
	ObserveTimeoutMs int
}

// AnchorBlockResult is the outcome of a bounded anchor check.
type AnchorBlockResult struct {
	Matched      bool                 `json:"matched"`
	AttemptCount int                  `json:"attemptCount"`
	LastResult   foundation.AnchorResult `json:"lastResult"`
	Nodes        []foundation.UiNode  `json:"nodes,omitempty"`
}

// DefaultAnchorConfig returns sensible defaults.
func DefaultAnchorConfig() AnchorConfig {
	return AnchorConfig{
		MaxRetries:       3,
		PollDelayFn:      foundation.AnchorPollDelay,
		ObserveTimeoutMs: 8000,
	}
}

// AnchorBlock evaluates an AnchorSpec with bounded retry.
func AnchorBlock(
	session *state.ClientSession,
	app *state.AppState,
	artifactRoot string,
	spec foundation.AnchorSpec,
	cfg AnchorConfig,
) (*AnchorBlockResult, error) {
	tr := MakeTransport(session, app)
	return anchorBlockWithTransport(tr, artifactRoot, spec, cfg)
}

func anchorBlockWithTransport(
	tr *Transport,
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
		obs, err := observePageWithTransport(tr, artifactRoot, cfg.ObserveTimeoutMs)
		if err != nil {
			lastResult = foundation.AnchorResult{
				Matched: false,
				Reasons: []string{fmt.Sprintf("observe failed on attempt %d: %v", attempt, err)},
			}
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
			}, nil
		}

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
