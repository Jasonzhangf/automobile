package blocks_test

import (
	"testing"

	"flowy/services/mac-daemon/src/blocks"
	"flowy/services/mac-daemon/src/foundation"
)

func makeCommentNodes() []foundation.UiNode {
	// Simulate: 2 comments, first not liked, second liked
	return []foundation.UiNode{
		// Comment 1: author
		{Text: "白墙", ClassName: "android.widget.TextView", Flags: foundation.NodeFlags{Clickable: true},
			BoundsInScreen: &foundation.Bounds{Left: 290, Top: 1436, Right: 388, Bottom: 1492}},
		// Comment 1: content
		{Text: "谢谢你呀！ 2天前 广西 回复", ClassName: "android.widget.TextView",
			BoundsInScreen: &foundation.Bounds{Left: 290, Top: 1492, Right: 1027, Bottom: 1632}},
		// Comment 1: like button LinearLayout
		{ClassName: "android.widget.LinearLayout", Flags: foundation.NodeFlags{Clickable: true},
			BoundsInScreen: &foundation.Bounds{Left: 1062, Top: 1415, Right: 1192, Bottom: 1572}},
		// Comment 1: like ImageView (not selected)
		{ClassName: "android.widget.ImageView", Flags: foundation.NodeFlags{Selected: false},
			BoundsInScreen: &foundation.Bounds{Left: 1062, Top: 1415, Right: 1192, Bottom: 1545}},
		// Comment 1: like count
		{Text: "8", ClassName: "android.widget.TextView",
			BoundsInScreen: &foundation.Bounds{Left: 1116, Top: 1527, Right: 1137, Bottom: 1572}},
		// Comment 2: author
		{Text: "服务记在心里", ClassName: "android.widget.TextView", Flags: foundation.NodeFlags{Clickable: true},
			BoundsInScreen: &foundation.Bounds{Left: 203, Top: 1898, Right: 497, Bottom: 1954}},
		// Comment 2: content
		{Text: "这个太可爱了。 昨天 22:34 湖南 回复", ClassName: "android.widget.TextView",
			BoundsInScreen: &foundation.Bounds{Left: 203, Top: 1954, Right: 1027, Bottom: 2101}},
		// Comment 2: like button LinearLayout
		{ClassName: "android.widget.LinearLayout", Flags: foundation.NodeFlags{Clickable: true},
			BoundsInScreen: &foundation.Bounds{Left: 1062, Top: 1723, Right: 1192, Bottom: 1880}},
		// Comment 2: like ImageView (selected = already liked)
		{ClassName: "android.widget.ImageView", Flags: foundation.NodeFlags{Selected: true},
			BoundsInScreen: &foundation.Bounds{Left: 1062, Top: 1723, Right: 1192, Bottom: 1853}},
		// Comment 2: like count
		{Text: "1", ClassName: "android.widget.TextView",
			BoundsInScreen: &foundation.Bounds{Left: 1117, Top: 1834, Right: 1137, Bottom: 1880}},
	}
}

func TestDetectCommentLikes_Count(t *testing.T) {
	nodes := makeCommentNodes()
	likes := blocks.DetectCommentLikes(nodes)
	if len(likes) != 2 {
		t.Fatalf("expected 2 comment like buttons, got %d", len(likes))
	}
}

func TestDetectCommentLikes_State(t *testing.T) {
	nodes := makeCommentNodes()
	likes := blocks.DetectCommentLikes(nodes)
	if likes[0].Selected {
		t.Fatal("comment 0 should not be selected")
	}
	if !likes[1].Selected {
		t.Fatal("comment 1 should be selected")
	}
	if likes[0].Count != 8 {
		t.Fatalf("comment 0 count expected 8, got %d", likes[0].Count)
	}
	if likes[1].Count != 1 {
		t.Fatalf("comment 1 count expected 1, got %d", likes[1].Count)
	}
}

func TestDetectCommentLikes_Coordinates(t *testing.T) {
	nodes := makeCommentNodes()
	likes := blocks.DetectCommentLikes(nodes)
	// First button center: (1062+1192)/2=1127, (1415+1572)/2=1493
	if likes[0].CenterX != 1127 {
		t.Fatalf("expected centerX=1127, got %d", likes[0].CenterX)
	}
	if likes[0].CenterY != 1493 {
		t.Fatalf("expected centerY=1493, got %d", likes[0].CenterY)
	}
}

func TestDetectCommentLikes_IgnoresSmallButtons(t *testing.T) {
	nodes := []foundation.UiNode{
		// A random clickable LinearLayout at left<1000 should be ignored
		{ClassName: "android.widget.LinearLayout", Flags: foundation.NodeFlags{Clickable: true},
			BoundsInScreen: &foundation.Bounds{Left: 100, Top: 100, Right: 200, Bottom: 200}},
	}
	likes := blocks.DetectCommentLikes(nodes)
	if len(likes) != 0 {
		t.Fatalf("expected 0 likes, got %d", len(likes))
	}
}
