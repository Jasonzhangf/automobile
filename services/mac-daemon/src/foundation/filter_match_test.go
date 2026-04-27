package foundation

import (
	"testing"
)

var testNodes = []UiNode{
	{Text: "", ClassName: "android.widget.FrameLayout", Flags: NodeFlags{Enabled: true}},
	{Text: "搜索", ClassName: "android.widget.Button", ContentDescription: "", Flags: NodeFlags{Clickable: true, Enabled: true},
		BoundsInScreen: &Bounds{Left: 100, Top: 50, Right: 200, Bottom: 90}},
	{Text: "", ClassName: "android.widget.EditText", Flags: NodeFlags{Editable: true, Enabled: true},
		BoundsInScreen: &Bounds{Left: 0, Top: 50, Right: 90, Bottom: 90}},
	{Text: "DeepSeek V4 教程", ClassName: "android.widget.TextView", ContentDescription: "",
		Flags: NodeFlags{Clickable: true, Enabled: true},
		BoundsInScreen: &Bounds{Left: 0, Top: 200, Right: 600, Bottom: 300}},
	{Text: "DeepSeek V4 教程", ClassName: "android.widget.TextView", ContentDescription: "",
		Flags: NodeFlags{Clickable: true, Enabled: true},
		BoundsInScreen: &Bounds{Left: 0, Top: 500, Right: 600, Bottom: 600}},
	{Text: "���击查看全部评论", ClassName: "android.widget.TextView", ContentDescription: "评论入口",
		Flags: NodeFlags{Clickable: true, Enabled: true},
		BoundsInScreen: &Bounds{Left: 0, Top: 800, Right: 600, Bottom: 850}},
	{Text: "", ClassName: "android.widget.Button", ContentDescription: "点赞 522",
		Flags: NodeFlags{Clickable: true, Selected: false, Enabled: true},
		BoundsInScreen: &Bounds{Left: 0, Top: 2400, Right: 100, Bottom: 2500}},
	{Text: "", ClassName: "android.widget.Button", ContentDescription: "评论 111",
		Flags: NodeFlags{Clickable: true, Selected: false, Enabled: true},
		BoundsInScreen: &Bounds{Left: 120, Top: 2400, Right: 220, Bottom: 2500}},
	{Text: "正文内容比较长的一段文字超过一百个字符用于测试minTextLength过滤条件是否正确生效这个应该足够长了吧还需要再加一些文字", ClassName: "android.widget.TextView",
		Flags: NodeFlags{Enabled: true},
		BoundsInScreen: &Bounds{Left: 0, Top: 300, Right: 600, Bottom: 700}},
	{Text: "", ClassName: "android.widget.ImageView", ContentDescription: "搜索图标",
		Flags: NodeFlags{Enabled: true},
		BoundsInScreen: &Bounds{Left: 10, Top: 10, Right: 50, Bottom: 50}},
}

func TestMatchNodes_ByClassName(t *testing.T) {
	spec := FilterSpec{ClassName: "android.widget.Button"}
	results := MatchNodes(testNodes, spec)
	// Button nodes: index 1 (搜索), 6 (点赞), 7 (评论)
	if len(results) != 3 {
		t.Fatalf("expected 3 button nodes, got %d", len(results))
	}
}

func TestMatchNodes_ByTextContains(t *testing.T) {
	spec := FilterSpec{TextContains: "DeepSeek"}
	results := MatchNodes(testNodes, spec)
	if len(results) != 2 {
		t.Fatalf("expected 2 DeepSeek nodes, got %d", len(results))
	}
}

func TestMatchNodes_ByDescMatches(t *testing.T) {
	spec := FilterSpec{DescMatches: "^点赞\\s*\\d+"}
	results := MatchNodes(testNodes, spec)
	if len(results) != 1 {
		t.Fatalf("expected 1 like button, got %d", len(results))
	}
	if results[0].Node.ContentDescription != "点赞 522" {
		t.Fatalf("expected '点赞 522', got '%s'", results[0].Node.ContentDescription)
	}
}

func TestMatchNodes_NotDesc(t *testing.T) {
	spec := FilterSpec{ClassName: "android.widget.TextView", NotDesc: true, MinTextLength: 5}
	results := MatchNodes(testNodes, spec)
	// Text nodes with non-empty desc: only index 5 (评论入口) should be excluded
	for _, r := range results {
		if r.Node.ContentDescription != "" {
			t.Fatalf("notDesc violation: node[%d] has desc '%s'", r.Index, r.Node.ContentDescription)
		}
	}
}

func TestMatchNodes_ClickableTrue(t *testing.T) {
	clickable := true
	spec := FilterSpec{Clickable: &clickable}
	results := MatchNodes(testNodes, spec)
	for _, r := range results {
		if !r.Node.Flags.Clickable {
			t.Fatalf("clickable violation: node[%d]", r.Index)
		}
	}
}

func TestMatchNodes_MinTextLength(t *testing.T) {
	spec := FilterSpec{MinTextLength: 100}
	results := MatchNodes(testNodes, spec)
	if len(results) != 1 {
		t.Fatalf("expected 1 long-text node, got %d", len(results))
	}
}

func TestMatchNodes_HasBounds(t *testing.T) {
	spec := FilterSpec{HasBounds: true, ClassName: "android.widget.TextView"}
	results := MatchNodes(testNodes, spec)
	for _, r := range results {
		if r.Node.BoundsInScreen == nil {
			t.Fatalf("hasBounds violation: node[%d]", r.Index)
		}
	}
}

func TestMatchNodes_SelectedFalse(t *testing.T) {
	selected := false
	spec := FilterSpec{DescMatches: "^点赞", Selected: &selected}
	results := MatchNodes(testNodes, spec)
	if len(results) != 1 {
		t.Fatalf("expected 1 unselected like button, got %d", len(results))
	}
	if results[0].Node.Flags.Selected {
		t.Fatal("expected selected=false")
	}
}

func TestSelectTargets_FirstBest(t *testing.T) {
	spec := FilterSpec{TextContains: "DeepSeek"}
	matches := MatchNodes(testNodes, spec)
	selected, idx := SelectTargets(matches, "first_best")
	if len(selected) != 1 {
		t.Fatalf("expected 1 selected, got %d", len(selected))
	}
	if idx != 0 {
		t.Fatalf("expected idx 0, got %d", idx)
	}
}

func TestSelectTargets_AllVisible(t *testing.T) {
	spec := FilterSpec{TextContains: "DeepSeek"}
	matches := MatchNodes(testNodes, spec)
	selected, _ := SelectTargets(matches, "all_visible")
	if len(selected) != 2 {
		t.Fatalf("expected 2, got %d", len(selected))
	}
}

func TestSelectTargets_TopMost(t *testing.T) {
	spec := FilterSpec{TextContains: "DeepSeek"}
	matches := MatchNodes(testNodes, spec)
	selected, _ := SelectTargets(matches, "top_most")
	if len(selected) != 2 {
		t.Fatalf("expected 2, got %d", len(selected))
	}
	// top-most should be the one with smaller Top value
	if selected[0].Node.BoundsInScreen.Top > selected[1].Node.BoundsInScreen.Top {
		t.Fatal("top_most should sort by ascending Top")
	}
}

func TestSelectTargets_LongestText(t *testing.T) {
	spec := FilterSpec{SelectStrategy: "longest_text", ClassName: "android.widget.TextView", NotDesc: true}
	matches := MatchNodes(testNodes, spec)
	selected, _ := SelectTargets(matches, "longest_text")
	if len(selected) == 0 {
		t.Fatal("expected at least 1 match")
	}
	// First should have longest text
	if len(selected[0].Node.Text) < len(selected[len(selected)-1].Node.Text) {
		t.Fatal("longest_text should sort descending by text length")
	}
}

func TestMatchNodes_Empty(t *testing.T) {
	spec := FilterSpec{ClassName: "android.widget.Button"}
	results := MatchNodes(nil, spec)
	if len(results) != 0 {
		t.Fatalf("expected 0, got %d", len(results))
	}
}

func TestSelectTargets_Empty(t *testing.T) {
	selected, idx := SelectTargets(nil, "first_best")
	if selected != nil || idx != 0 {
		t.Fatal("expected nil, 0 for empty matches")
	}
}
