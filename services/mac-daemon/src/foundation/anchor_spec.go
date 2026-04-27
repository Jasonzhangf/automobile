 package foundation
 
import "strings"

// AnchorSpec describes a set of conditions that must hold true on a page.
// Used for both pre-anchor ("is this the right page before I act?") and
// post-anchor ("did the action land on the expected page?").
type AnchorSpec struct {
	AnchorID           string   `json:"anchorId"`
	MustContainTexts   []string `json:"mustContainTexts,omitempty"`
	MustNotContainTexts []string `json:"mustNotContainTexts,omitempty"`
	TargetVisible      *bool    `json:"targetVisible,omitempty"`    // check if target node is in nodes
	TargetDescContains string   `json:"targetDescContains,omitempty"`
	TargetTextContains string   `json:"targetTextContains,omitempty"`
	MinNodeCount       int      `json:"minNodeCount,omitempty"`     // minimum total nodes on page
	MaxNodeCount       int      `json:"maxNodeCount,omitempty"`     // maximum total nodes on page
	PackageName        string   `json:"packageName,omitempty"`      // expected foreground package
}

// AnchorResult is the evaluation outcome of an AnchorSpec.
type AnchorResult struct {
	Matched bool     `json:"matched"`
	Reasons []string `json:"reasons"`
}

// EvaluateAnchor checks a set of UiNodes against an AnchorSpec.
// Returns AnchorResult with human-readable reasons for each check.
func EvaluateAnchor(nodes []UiNode, spec AnchorSpec) AnchorResult {
	var pass []string
	var fail []string

	// mustContainTexts: every text must appear in at least one node
	for _, want := range spec.MustContainTexts {
		if anyNodeContainsText(nodes, want) {
			pass = append(pass, "mustContainTexts["+want+"] satisfied")
		} else {
			fail = append(fail, "mustContainTexts["+want+"] missing")
		}
	}

	// mustNotContainTexts: no node should contain the text
	for _, ban := range spec.MustNotContainTexts {
		if anyNodeContainsText(nodes, ban) {
			fail = append(fail, "mustNotContainTexts["+ban+"] found (should not exist)")
		} else {
			pass = append(pass, "mustNotContainTexts["+ban+"] satisfied")
		}
	}

	// targetVisible: check if target node exists by desc or text
	if spec.TargetVisible != nil && *spec.TargetVisible {
		if findTargetNode(nodes, spec.TargetDescContains, spec.TargetTextContains) != nil {
			pass = append(pass, "targetVisible satisfied")
		} else {
			fail = append(fail, "targetVisible: target not found")
		}
	}

	// node count bounds
	if spec.MinNodeCount > 0 {
		if len(nodes) >= spec.MinNodeCount {
			pass = append(pass, "minNodeCount satisfied")
		} else {
			fail = append(fail, "minNodeCount not met")
		}
	}
	if spec.MaxNodeCount > 0 {
		if len(nodes) <= spec.MaxNodeCount {
			pass = append(pass, "maxNodeCount satisfied")
		} else {
			fail = append(fail, "maxNodeCount exceeded")
		}
	}

	// packageName: match against nodes' package names
	if spec.PackageName != "" {
		if anyNodeHasPackage(nodes, spec.PackageName) {
			pass = append(pass, "packageName["+spec.PackageName+"] matched")
		} else {
			fail = append(fail, "packageName["+spec.PackageName+"] not found")
		}
	}

	return AnchorResult{
		Matched: len(fail) == 0 && len(pass) > 0,
		Reasons: append(pass, fail...),
	}
}

func anyNodeContainsText(nodes []UiNode, text string) bool {
	for _, n := range nodes {
		if strings.Contains(n.Text, text) || strings.Contains(n.ContentDescription, text) {
			return true
		}
	}
	return false
}

func findTargetNode(nodes []UiNode, descContains, textContains string) *UiNode {
	for i := range nodes {
		n := &nodes[i]
		descOk := descContains == "" || strings.Contains(n.ContentDescription, descContains)
		textOk := textContains == "" || strings.Contains(n.Text, textContains)
		if descOk && textOk && (descContains != "" || textContains != "") {
			return n
		}
	}
	return nil
}

func anyNodeHasPackage(nodes []UiNode, pkg string) bool {
	for _, n := range nodes {
		if n.PackageName == pkg {
			return true
		}
	}
	return false
}
