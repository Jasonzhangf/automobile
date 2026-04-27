package foundation

// NodeFlags represents boolean capability flags for a UI node.
type NodeFlags struct {
	Clickable     bool `json:"clickable"`
	Scrollable    bool `json:"scrollable"`
	Editable      bool `json:"editable"`
	Checkable     bool `json:"checkable"`
	Checked       bool `json:"checked"`
	Enabled       bool `json:"enabled"`
	Focusable     bool `json:"focusable"`
	Focused       bool `json:"focused"`
	LongClickable bool `json:"longClickable"`
	Selected      bool `json:"selected"`
	Password      bool `json:"password"`
}

// Bounds represents a screen rectangle [left, top, right, bottom].
type Bounds struct {
	Left   int `json:"left"`
	Top    int `json:"top"`
	Right  int `json:"right"`
	Bottom int `json:"bottom"`
}

// Center returns the center point of the bounds.
func (b Bounds) Center() (int, int) {
	return (b.Left + b.Right) / 2, (b.Top + b.Bottom) / 2
}

// Width returns the width of the bounds.
func (b Bounds) Width() int { return b.Right - b.Left }

// Height returns the height of the bounds.
func (b Bounds) Height() int { return b.Bottom - b.Top }

// UiNode represents a single node from a UI tree dump.
type UiNode struct {
	Text               string   `json:"text"`
	ContentDescription string   `json:"contentDescription"`
	HintText           string   `json:"hintText"`
	ClassName          string   `json:"className"`
	PackageName        string   `json:"packageName"`
	ResourceID         string   `json:"resourceId"`
	Flags              NodeFlags `json:"flags"`
	BoundsInScreen     *Bounds  `json:"boundsInScreen,omitempty"`
}

// UiTreeDump represents the top-level structure of a root-ui-tree.json artifact.
type UiTreeDump struct {
	PackageName string   `json:"packageName"`
	Nodes       []UiNode `json:"nodes"`
}
