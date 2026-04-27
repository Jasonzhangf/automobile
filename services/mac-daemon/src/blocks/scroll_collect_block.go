package blocks

import (
	"fmt"
	"time"

	"flowy/services/mac-daemon/src/foundation"
	"flowy/services/mac-daemon/src/state"
)

// ScrollCollectConfig controls the scroll-and-collect loop.
type ScrollCollectConfig struct {
	MaxScrolls    int    `json:"maxScrolls"`    // max scroll attempts (0 = default 30)
	Backend       string `json:"backend"`       // "root" or "accessibility"
	ScrollDelayMs int    `json:"scrollDelayMs"` // min delay between scrolls (default 800)
	ScrollJitterMs int   `json:"scrollJitterMs"` // random jitter added to delay (default 700)
	TimeoutMs     int    `json:"timeoutMs"`     // per-command timeout
}

// DefaultScrollCollectConfig returns sensible defaults.
func DefaultScrollCollectConfig() ScrollCollectConfig {
	return ScrollCollectConfig{
		MaxScrolls:     30,
		Backend:        "root",
		ScrollDelayMs:  800,
		ScrollJitterMs: 700,
		TimeoutMs:      15000,
	}
}

// ScrollCollectResult is the output of a scroll-collect run.
type ScrollCollectResult struct {
	AllComments  []foundation.CommentEntry `json:"allComments"`
	ScrollsDone  int                       `json:"scrollsDone"`
	BottomSignal foundation.BottomSignal   `json:"bottomSignal"`
	BottomReason string                    `json:"bottomReason"`
	AllTexts     []string                  `json:"allTexts"` // all unique texts seen
}

// ScrollCollectBlock runs the scroll-dump-extract loop on a detail page.
// It scrolls through the page collecting all visible comments/texts until
// bottom is detected or maxScrolls is reached.
func ScrollCollectBlock(
	session *state.ClientSession,
	app *state.AppState,
	artifactRoot string,
	cfg ScrollCollectConfig,
) (*ScrollCollectResult, error) {
	if cfg.MaxScrolls <= 0 {
		cfg.MaxScrolls = 30
	}
	if cfg.TimeoutMs <= 0 {
		cfg.TimeoutMs = 15000
	}
	backend := OperationBackend(cfg.Backend)
	if backend == "" {
		backend = BackendRoot
	}

	detector := foundation.NewBottomDetector()
	allComments := make([]foundation.CommentEntry, 0)
	seenTexts := make(map[string]bool)

	for round := 0; round < cfg.MaxScrolls; round++ {
		// 1. Observe current page
		obs, err := ObservePage(session, app, artifactRoot, cfg.TimeoutMs)
		if err != nil {
			if round == 0 {
				return nil, fmt.Errorf("scroll-collect: initial observe failed: %w", err)
			}
			// Non-fatal: retry next round after delay
			jitterSleep(cfg.ScrollDelayMs, cfg.ScrollJitterMs)
			continue
		}

		// 2. Extract comments from current view
		comments := foundation.ExtractComments(obs.Nodes)
		for _, c := range comments {
			key := c.Author + "|" + c.Content
			if !seenTexts[key] {
				seenTexts[key] = true
				allComments = append(allComments, c)
			}
		}

		// 3. Collect all unique texts
		for _, n := range obs.Nodes {
			t := n.Text
			if t != "" && !seenTexts[t] {
				seenTexts[t] = true
			}
		}

		// 4. Check bottom
		isBottom, signal, reason := detector.Check(obs.Nodes)
		if isBottom {
			allTexts := make([]string, 0, len(seenTexts))
			for k := range seenTexts {
				allTexts = append(allTexts, k)
			}
			return &ScrollCollectResult{
				AllComments:  allComments,
				ScrollsDone:  round + 1,
				BottomSignal: signal,
				BottomReason: reason,
				AllTexts:     allTexts,
			}, nil
		}

		// 5. Scroll forward
		if err := ScrollBlock(session, app, artifactRoot,
			ScrollTarget{}, "forward", backend, cfg.TimeoutMs); err != nil {
			// Scroll failed — could be at bottom or error
			if round > 0 {
				// Try one more observe to confirm
				break
			}
			return nil, fmt.Errorf("scroll-collect: scroll failed: %w", err)
		}

		// 6. Random delay
		jitterSleep(cfg.ScrollDelayMs, cfg.ScrollJitterMs)
	}

	// Max scrolls reached
	allTexts := make([]string, 0, len(seenTexts))
	for k := range seenTexts {
		allTexts = append(allTexts, k)
	}
	return &ScrollCollectResult{
		AllComments: allComments,
		ScrollsDone: cfg.MaxScrolls,
		AllTexts:    allTexts,
	}, nil
}

func jitterSleep(baseMs, jitterMs int) {
	time.Sleep(foundation.RandomDuration(baseMs, baseMs+jitterMs))
}
