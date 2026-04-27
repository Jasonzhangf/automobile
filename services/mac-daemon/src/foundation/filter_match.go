package foundation

import (
	"sort"
	"strings"
)

// MatchNodes applies a FilterSpec against a list of UiNodes and returns
// all matches. The caller applies the selection strategy.
func MatchNodes(nodes []UiNode, spec FilterSpec) []MatchResult {
	var results []MatchResult
	for i, node := range nodes {
		if matchesFilter(node, spec) {
			score := 0
			if spec.SelectStrategy == "longest_text" {
				score = len(node.Text)
			}
			results = append(results, MatchResult{
				Node:  node,
				Index: i,
				Score: score,
			})
		}
	}
	return results
}

// SelectTargets applies the selection strategy to matched results.
// Returns the selected slice and the selected index (0-based within the returned slice).
func SelectTargets(matches []MatchResult, strategy string) ([]MatchResult, int) {
	if len(matches) == 0 {
		return nil, 0
	}
	switch strategy {
	case "longest_text":
		sort.Slice(matches, func(i, j int) bool {
			return matches[i].Score > matches[j].Score
		})
		return matches, 0
	case "top_most":
		sort.Slice(matches, func(i, j int) bool {
			bi := matches[i].Node.BoundsInScreen
			bj := matches[j].Node.BoundsInScreen
			if bi == nil && bj == nil {
				return matches[i].Index < matches[j].Index
			}
			if bi == nil {
				return false
			}
			if bj == nil {
				return true
			}
			return bi.Top < bj.Top
		})
		return matches, 0
	case "all_visible":
		return matches, 0
	default: // first_best
		return matches[:1], 0
	}
}

// matchesFilter checks whether a single node satisfies all non-empty constraints
// of the filter. Empty constraint fields are skipped (no constraint).
func matchesFilter(node UiNode, spec FilterSpec) bool {
	// className exact match
	if spec.ClassName != "" {
		if node.ClassName != spec.ClassName {
			return false
		}
	}

	// text contains
	if spec.TextContains != "" {
		if !strings.Contains(node.Text, spec.TextContains) {
			return false
		}
	}

	// text regex
	if spec.TextMatches != "" {
		if !textMatchesRegex(node.Text, spec.TextMatches) {
			return false
		}
	}

	// desc contains
	if spec.DescContains != "" {
		if !strings.Contains(node.ContentDescription, spec.DescContains) {
			return false
		}
	}

	// desc regex
	if spec.DescMatches != "" {
		if !textMatchesRegex(node.ContentDescription, spec.DescMatches) {
			return false
		}
	}

	// notDesc: node must have empty content description
	if spec.NotDesc && node.ContentDescription != "" {
		return false
	}

	// minTextLength
	if spec.MinTextLength > 0 {
		if len(node.Text) < spec.MinTextLength {
			return false
		}
	}

	// maxTextLength
	if spec.MaxTextLength > 0 {
		if len(node.Text) > spec.MaxTextLength {
			return false
		}
	}

	// boolean flags: only check if pointer is non-nil
	if spec.Clickable != nil && node.Flags.Clickable != *spec.Clickable {
		return false
	}
	if spec.Editable != nil && node.Flags.Editable != *spec.Editable {
		return false
	}
	if spec.Scrollable != nil && node.Flags.Scrollable != *spec.Scrollable {
		return false
	}
	if spec.Selected != nil && node.Flags.Selected != *spec.Selected {
		return false
	}

	// hasBounds: node must have non-nil bounds
	if spec.HasBounds && node.BoundsInScreen == nil {
		return false
	}

	return true
}
