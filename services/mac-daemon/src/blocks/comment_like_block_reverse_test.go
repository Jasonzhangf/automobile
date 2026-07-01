package blocks_test

import (
	"testing"

	"flowy/services/mac-daemon/src/blocks"
	"flowy/services/mac-daemon/src/foundation"
)

// --- Reverse tests: verify what will NOT happen ---

func TestDetectCommentLikes_EmptyNodes_ReturnsEmpty(t *testing.T) {
	likes := blocks.DetectCommentLikes(nil)
	if len(likes) != 0 {
		t.Fatalf("expected 0 likes for nil nodes, got %d", len(likes))
	}
}

func TestDetectCommentLikes_NoLinearLayout_IgnoresNonLinear(t *testing.T) {
	nodes := []foundation.UiNode{
		{ClassName: "android.widget.TextView", Text: "点赞 5",
			Flags: foundation.NodeFlags{Clickable: true},
			BoundsInScreen: &foundation.Bounds{Left: 1000, Top: 100, Right: 1200, Bottom: 200}},
	}
	likes := blocks.DetectCommentLikes(nodes)
	if len(likes) != 0 {
		t.Fatalf("expected 0 likes for non-LinearLayout, got %d", len(likes))
	}
}

func TestDetectCommentLikes_WrongSide_Ignored(t *testing.T) {
	nodes := []foundation.UiNode{
		// LinearLayout on the LEFT side (left < 1000) — should NOT be detected
		{ClassName: "android.widget.LinearLayout", Flags: foundation.NodeFlags{Clickable: true},
			BoundsInScreen: &foundation.Bounds{Left: 100, Top: 100, Right: 200, Bottom: 200}},
	}
	likes := blocks.DetectCommentLikes(nodes)
	if len(likes) != 0 {
		t.Fatalf("expected 0 likes for left-side LinearLayout, got %d", len(likes))
	}
}

func TestDetectCommentLikes_NoBounds_Ignored(t *testing.T) {
	nodes := []foundation.UiNode{
		{ClassName: "android.widget.LinearLayout", Flags: foundation.NodeFlags{Clickable: true},
			BoundsInScreen: nil},
	}
	likes := blocks.DetectCommentLikes(nodes)
	if len(likes) != 0 {
		t.Fatalf("expected 0 likes for nil bounds, got %d", len(likes))
	}
}
