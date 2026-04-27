package flows

import "flowy/services/mac-daemon/src/foundation"

// --- Collection flow types --------------------------------------------

// CollectionMode selects how to enter the list.
type CollectionMode string

const (
	ModeSearch   CollectionMode = "search"
	ModeTimeline CollectionMode = "timeline"
)

// CollectionConfig is the task-level configuration.
type CollectionConfig struct {
	Mode          CollectionMode `json:"mode"`
	Keyword       string         `json:"keyword,omitempty"`       // search keyword
	TargetCount   int            `json:"targetCount"`             // how many items to collect
	EnableLike    bool           `json:"enableLike,omitempty"`
	EnableFav     bool           `json:"enableFav,omitempty"`
	EnableComments bool          `json:"enableComments,omitempty"`
	MaxComments   int            `json:"maxComments,omitempty"`   // 0 = unlimited
	Backend       string         `json:"backend"`                 // "accessibility" or "root"
}

// CollectionProfile describes app-specific selectors and anchors.
type CollectionProfile struct {
	AppID        string               `json:"appId"`
	PackageName  string               `json:"packageName"`
	// List anchors
	ListAnchor   foundation.AnchorSpec `json:"listAnchor"`
	DetailAnchor foundation.AnchorSpec `json:"detailAnchor"`
	// List item filter
	ItemFilter   foundation.FilterSpec `json:"itemFilter"`
	// Detail page fields (for snapshot extraction)
	DetailFields []DetailField        `json:"detailFields,omitempty"`
	// Comment panel anchor
	CommentAnchor *foundation.AnchorSpec `json:"commentAnchor,omitempty"`
}

// DetailField describes a single field to extract from a detail page.
type DetailField struct {
	Name        string            `json:"name"`
	Source      string            `json:"source"` // "text", "contentDescription", "child_text"
	Filter      *foundation.FilterSpec `json:"filter,omitempty"`
}

// CollectionResult holds the result of a single collected item.
type CollectionResult struct {
	ItemID     string                 `json:"itemId"`
	Title      string                 `json:"title,omitempty"`
	CollectedAt string               `json:"collectedAt"`
	Fields     map[string]string      `json:"fields,omitempty"`
	Comments   []CommentEntry         `json:"comments,omitempty"`
	Actions    []string               `json:"actions,omitempty"` // e.g. "liked", "fav'd"
}

// CommentEntry is a single comment from the comment panel.
type CommentEntry struct {
	Author      string `json:"author"`
	Content     string `json:"content"`
	Time        string `json:"time,omitempty"`
	Liked       bool   `json:"liked,omitempty"`
}

// CollectionRunResult is the full run output.
type CollectionRunResult struct {
	Status       string              `json:"status"` // "completed", "partial", "aborted"
	TotalItems   int                 `json:"totalItems"`
	SuccessItems int                 `json:"successItems"`
	SkippedItems int                 `json:"skippedItems"`
	FailedItems  int                 `json:"failedItems"`
	Items        []CollectionResult  `json:"items"`
	Error        string              `json:"error,omitempty"`
}
