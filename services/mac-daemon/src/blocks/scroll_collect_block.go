package blocks

import (
	"fmt"
	"time"

	"flowy/services/mac-daemon/src/foundation"
	"flowy/services/mac-daemon/src/state"
)

// ScrollCollectConfig controls the scroll-and-collect loop.
type ScrollCollectConfig struct {
	MaxScrolls     int    `json:"maxScrolls"`
	Backend        string `json:"backend"`
	ScrollDelayMs  int    `json:"scrollDelayMs"`
	ScrollJitterMs int    `json:"scrollJitterMs"`
	TimeoutMs      int    `json:"timeoutMs"`
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
	AllTexts     []string                  `json:"allTexts"`
}

// ScrollCollectBlock runs the scroll-dump-extract loop.
func ScrollCollectBlock(
	session *state.ClientSession,
	app *state.AppState,
	artifactRoot string,
	cfg ScrollCollectConfig,
) (*ScrollCollectResult, error) {
	tr := MakeTransport(session, app)
	return scrollCollectBlockWithTransport(tr, artifactRoot, cfg)
}

func scrollCollectBlockWithTransport(
	tr *Transport,
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
		obs, err := observePageWithTransport(tr, artifactRoot, cfg.TimeoutMs)
		if err != nil {
			if round == 0 {
				return nil, fmt.Errorf("scroll-collect: initial observe failed: %w", err)
			}
			time.Sleep(foundation.RandomDuration(cfg.ScrollDelayMs, cfg.ScrollDelayMs+cfg.ScrollJitterMs))
			continue
		}

		comments := foundation.ExtractComments(obs.Nodes)
		for _, c := range comments {
			key := c.Author + "|" + c.Content
			if !seenTexts[key] {
				seenTexts[key] = true
				allComments = append(allComments, c)
			}
		}

		for _, n := range obs.Nodes {
			t := n.Text
			if t != "" && !seenTexts[t] {
				seenTexts[t] = true
			}
		}

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

		if err := scrollWithTransport(tr, artifactRoot, ScrollTarget{}, "forward", backend, cfg.TimeoutMs); err != nil {
			if round > 0 {
				break
			}
			return nil, fmt.Errorf("scroll-collect: scroll failed: %w", err)
		}

		time.Sleep(foundation.RandomDuration(cfg.ScrollDelayMs, cfg.ScrollDelayMs+cfg.ScrollJitterMs))
	}

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
