package foundation

import (
	"crypto/sha256"
	"fmt"
	"sort"
	"strings"
)

// BottomSignal represents how the bottom was detected.
type BottomSignal string

const (
	SignalNone         BottomSignal = ""
	SignalTextRepeated BottomSignal = "text_repeated" // Signal A: text set identical to previous
	SignalTailMarker   BottomSignal = "tail_marker"   // Signal B: known "no more" text found
	SignalNoNewText    BottomSignal = "no_new_text"   // Signal C: same node count, zero new text
)

// Tail markers that indicate end of content.
var tailMarkers = []string{
	"暂时没有更多了",
	"没有更多评论",
	"已经到底了",
	"没有更多了",
	"到底了",
}

// BottomDetector tracks state across scroll+detect cycles.
type BottomDetector struct {
	prevTextSet   map[string]bool
	prevTextHash  string
	prevNodeCount int
	newTextSeen   int
	rounds        int
}

// NewBottomDetector creates a fresh detector.
func NewBottomDetector() *BottomDetector {
	return &BottomDetector{
		prevTextSet: make(map[string]bool),
	}
}

// Check evaluates the current page nodes for bottom signals.
// Returns (isBottom, signal, reason).
func (bd *BottomDetector) Check(nodes []UiNode) (bool, BottomSignal, string) {
	bd.rounds++

	// Extract all non-empty text from nodes
	currentTexts := make(map[string]bool)
	for _, n := range nodes {
		t := strings.TrimSpace(n.Text)
		if t != "" {
			currentTexts[t] = true
		}
	}
	currentHash := textSetHash(currentTexts)

	// Signal B: check tail markers first (highest confidence)
	for _, n := range nodes {
		t := strings.TrimSpace(n.Text)
		for _, marker := range tailMarkers {
			if t == marker {
				return true, SignalTailMarker, fmt.Sprintf("tail marker found: %q", t)
			}
		}
	}

	// On first round, just record baseline
	if bd.rounds == 1 {
		bd.prevTextSet = currentTexts
		bd.prevTextHash = currentHash
		bd.prevNodeCount = len(nodes)
		bd.newTextSeen = len(currentTexts)
		return false, SignalNone, "first round baseline"
	}

	// Signal A: text set identical to previous
	if currentHash == bd.prevTextHash && len(currentTexts) > 0 {
		return true, SignalTextRepeated, "exact text set repeat detected"
	}

	// Signal C: node count unchanged AND zero new text
	newCount := 0
	for t := range currentTexts {
		if !bd.prevTextSet[t] {
			newCount++
		}
	}
	if newCount == 0 && len(nodes) == bd.prevNodeCount && len(nodes) > 0 {
		return true, SignalNoNewText, fmt.Sprintf("no new text, node count unchanged (%d)", len(nodes))
	}

	// Update state for next round
	bd.prevTextSet = currentTexts
	bd.prevTextHash = currentHash
	bd.prevNodeCount = len(nodes)
	bd.newTextSeen += newCount
	return false, SignalNone, fmt.Sprintf("%d new texts found", newCount)
}

// Rounds returns how many rounds have been evaluated.
func (bd *BottomDetector) Rounds() int { return bd.rounds }

// textSetHash produces a deterministic hash of a text set.
func textSetHash(texts map[string]bool) string {
	if len(texts) == 0 {
		return ""
	}
	sorted := make([]string, 0, len(texts))
	for t := range texts {
		sorted = append(sorted, t)
	}
	sort.Strings(sorted)
	combined := strings.Join(sorted, "\x00")
	return fmt.Sprintf("%x", sha256.Sum256([]byte(combined)))
}
