package foundation

import "regexp"

// FilterSpec describes what to look for on a page.
// Match fields are AND-constrained: a node must satisfy ALL non-empty fields.
type FilterSpec struct {
	FilterID       string   `json:"filterId"`
	Kind           string   `json:"kind"` // actionable, scrollable, input, card_like, image_like, blocker
	ClassName      string   `json:"className,omitempty"`
	TextContains   string   `json:"textContains,omitempty"`
	TextMatches    string   `json:"textMatches,omitempty"` // regex
	DescContains   string   `json:"descContains,omitempty"`
	DescMatches    string   `json:"descMatches,omitempty"` // regex
	NotDesc        bool     `json:"notDesc,omitempty"`     // true = only nodes with empty desc
	MinTextLength  int      `json:"minTextLength,omitempty"`
	MaxTextLength  int      `json:"maxTextLength,omitempty"`
	Clickable      *bool    `json:"clickable,omitempty"`
	Editable       *bool    `json:"editable,omitempty"`
	Scrollable     *bool    `json:"scrollable,omitempty"`
	Selected       *bool    `json:"selected,omitempty"`
	HasBounds      bool     `json:"hasBounds,omitempty"` // true = node must have bounds
	SelectStrategy string   `json:"selectStrategy,omitempty"` // first_best, top_most, longest_text, all_visible
}

// MatchResult holds a matched node and its computed score for selection.
type MatchResult struct {
	Node  UiNode
	Index int // original index in the node list
	Score int // used by select strategies (e.g. text length for longest_text)
}

// textMatchesRegex checks if text matches the given pattern string.
func textMatchesRegex(text, pattern string) bool {
	re, err := regexp.Compile(pattern)
	if err != nil {
		return false
	}
	return re.MatchString(text)
}
