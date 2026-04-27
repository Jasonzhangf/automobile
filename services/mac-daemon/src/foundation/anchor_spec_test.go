package foundation

import (
	"encoding/json"
	"os"
	"path/filepath"
	"testing"
)

func boolPtr(b bool) *bool { return &b }

// --- helpers -----------------------------------------------------------

func nodesFromJson(t *testing.T, path string) []UiNode {
	t.Helper()
	data, err := os.ReadFile(path)
	if err != nil {
		t.Fatalf("read %s: %v", path, err)
	}
	var dump UiTreeDump
	if err := json.Unmarshal(data, &dump); err != nil {
		t.Fatalf("parse %s: %v", path, err)
	}
	return dump.Nodes
}

// --- unit tests --------------------------------------------------------

func TestEvaluateAnchor_AllPass(t *testing.T) {
	nodes := []UiNode{
		{Text: "搜索", ClassName: "TextView"},
		{Text: "发现", ClassName: "TextView"},
	}
	spec := AnchorSpec{
		AnchorID:         "search_page",
		MustContainTexts: []string{"搜索", "发现"},
	}
	r := EvaluateAnchor(nodes, spec)
	if !r.Matched {
		t.Fatalf("expected matched, got false; reasons=%v", r.Reasons)
	}
}

func TestEvaluateAnchor_MissingText(t *testing.T) {
	nodes := []UiNode{
		{Text: "搜索", ClassName: "TextView"},
	}
	spec := AnchorSpec{
		AnchorID:         "search_page",
		MustContainTexts: []string{"搜索", "发现"},
	}
	r := EvaluateAnchor(nodes, spec)
	if r.Matched {
		t.Fatalf("expected NOT matched because '发现' missing")
	}
}

func TestEvaluateAnchor_MustNotContain(t *testing.T) {
	nodes := []UiNode{
		{Text: "搜索", ClassName: "TextView"},
	}
	spec := AnchorSpec{
		AnchorID:            "search_page",
		MustContainTexts:    []string{"搜索"},
		MustNotContainTexts: []string{"错误"},
	}
	r := EvaluateAnchor(nodes, spec)
	if !r.Matched {
		t.Fatalf("expected matched; reasons=%v", r.Reasons)
	}
}

func TestEvaluateAnchor_MustNotContain_Fail(t *testing.T) {
	nodes := []UiNode{
		{Text: "搜索", ClassName: "TextView"},
		{Text: "网络错误", ClassName: "TextView"},
	}
	spec := AnchorSpec{
		AnchorID:            "search_page",
		MustContainTexts:    []string{"搜索"},
		MustNotContainTexts: []string{"网络错误"},
	}
	r := EvaluateAnchor(nodes, spec)
	if r.Matched {
		t.Fatalf("expected NOT matched because '网络错误' present")
	}
}

func TestEvaluateAnchor_TargetVisible(t *testing.T) {
	nodes := []UiNode{
		{Text: "", ContentDescription: "点赞 522", ClassName: "Button"},
	}
	spec := AnchorSpec{
		AnchorID:           "detail_page",
		TargetVisible:      boolPtr(true),
		TargetDescContains: "点赞",
	}
	r := EvaluateAnchor(nodes, spec)
	if !r.Matched {
		t.Fatalf("expected matched; reasons=%v", r.Reasons)
	}
}

func TestEvaluateAnchor_TargetVisible_Missing(t *testing.T) {
	nodes := []UiNode{
		{Text: "关注", ClassName: "Button"},
	}
	spec := AnchorSpec{
		AnchorID:           "detail_page",
		TargetVisible:      boolPtr(true),
		TargetDescContains: "点赞",
	}
	r := EvaluateAnchor(nodes, spec)
	if r.Matched {
		t.Fatalf("expected NOT matched: target not visible")
	}
}

func TestEvaluateAnchor_NodeCount(t *testing.T) {
	nodes := []UiNode{
		{Text: "A"}, {Text: "B"}, {Text: "C"},
	}
	spec := AnchorSpec{
		AnchorID:     "any_page",
		MinNodeCount: 2,
		MaxNodeCount: 5,
	}
	r := EvaluateAnchor(nodes, spec)
	if !r.Matched {
		t.Fatalf("expected matched; reasons=%v", r.Reasons)
	}
}

func TestEvaluateAnchor_NodeCount_Exceeded(t *testing.T) {
	nodes := []UiNode{
		{Text: "A"}, {Text: "B"}, {Text: "C"},
	}
	spec := AnchorSpec{
		AnchorID:     "any_page",
		MaxNodeCount: 2,
	}
	r := EvaluateAnchor(nodes, spec)
	if r.Matched {
		t.Fatalf("expected NOT matched: node count exceeded")
	}
}

func TestEvaluateAnchor_PackageName(t *testing.T) {
	nodes := []UiNode{
		{Text: "搜索", PackageName: "com.xingin.xhs"},
	}
	spec := AnchorSpec{
		AnchorID:    "xhs_page",
		PackageName: "com.xingin.xhs",
	}
	r := EvaluateAnchor(nodes, spec)
	if !r.Matched {
		t.Fatalf("expected matched; reasons=%v", r.Reasons)
	}
}

func TestEvaluateAnchor_EmptySpec(t *testing.T) {
	nodes := []UiNode{{Text: "任意"}}
	spec := AnchorSpec{AnchorID: "empty"}
	r := EvaluateAnchor(nodes, spec)
	// No constraints = no pass reasons → matched=false
	if r.Matched {
		t.Fatalf("expected NOT matched for empty spec (no checks)")
	}
}

func TestEvaluateAnchor_ContentDescription(t *testing.T) {
	nodes := []UiNode{
		{ContentDescription: "已点赞 523", ClassName: "Button"},
	}
	spec := AnchorSpec{
		AnchorID:         "liked",
		MustContainTexts: []string{"已点赞"},
	}
	r := EvaluateAnchor(nodes, spec)
	if !r.Matched {
		t.Fatalf("expected matched via contentDescription; reasons=%v", r.Reasons)
	}
}

func TestEvaluateAnchor_JSONRoundTrip(t *testing.T) {
	spec := AnchorSpec{
		AnchorID:           "detail_anchor",
		MustContainTexts:   []string{"点赞", "收藏"},
		MustNotContainTexts: []string{"错误"},
		TargetVisible:      boolPtr(true),
		TargetDescContains: "点赞",
		MinNodeCount:       10,
		MaxNodeCount:       200,
		PackageName:        "com.xingin.xhs",
	}
	data, err := json.Marshal(spec)
	if err != nil {
		t.Fatalf("marshal: %v", err)
	}
	var roundTripped AnchorSpec
	if err := json.Unmarshal(data, &roundTripped); err != nil {
		t.Fatalf("unmarshal: %v", err)
	}
	if roundTripped.AnchorID != spec.AnchorID {
		t.Fatalf("anchorId mismatch: %s vs %s", roundTripped.AnchorID, spec.AnchorID)
	}
	if len(roundTripped.MustContainTexts) != 2 {
		t.Fatalf("mustContainTexts length mismatch")
	}
}

func TestEvaluateAnchor_WithRealDump(t *testing.T) {
	fixture := filepath.Join("..", "..", "..", "packages", "regression-fixtures", "xhs", "detail-page-nodes.json")
	if _, err := os.Stat(fixture); os.IsNotExist(err) {
		t.Skip("real dump fixture not available")
	}
	nodes := nodesFromJson(t, fixture)
	spec := AnchorSpec{
		AnchorID:         "xhs_detail",
		MustContainTexts: []string{"点赞"},
		MinNodeCount:     30,
		PackageName:      "com.xingin.xhs",
	}
	r := EvaluateAnchor(nodes, spec)
	if !r.Matched {
		t.Fatalf("expected matched on real dump; reasons=%v", r.Reasons)
	}
}
