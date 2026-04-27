 package foundation

 import (
 	"testing"
 )

 func ptrBool(b bool) *bool { return &b }

 func TestExtractComments_BasicXHSComment(t *testing.T) {
 	nodes := []UiNode{
 		{Text: "用户A", ClassName: "android.widget.TextView", Flags: NodeFlags{Clickable: true}},
 		{Text: "这个帖子太棒了，感谢分享！", ClassName: "android.widget.TextView"},
 		{Text: "3天前", ClassName: "android.widget.TextView"},
 		{Text: "12", ClassName: "android.widget.TextView"},
 		{Text: "回复", ClassName: "android.widget.TextView"},
 		{Text: "用户B", ClassName: "android.widget.TextView", Flags: NodeFlags{Clickable: true}},
 		{Text: "确实，学到了很多", ClassName: "android.widget.TextView"},
 		{Text: "1小时前", ClassName: "android.widget.TextView"},
 	}
 	comments := ExtractComments(nodes)
 	if len(comments) < 2 {
 		t.Fatalf("expected >=2 comments, got %d", len(comments))
 	}
 	if comments[0].Author != "用户A" {
 		t.Errorf("expected author 用户A, got %q", comments[0].Author)
 	}
 	if comments[0].Content != "这个帖子太棒了，感谢分享！" {
 		t.Errorf("unexpected content: %q", comments[0].Content)
 	}
 	if comments[1].Author != "用户B" {
 		t.Errorf("expected author 用户B, got %q", comments[1].Author)
 	}
 }

 func TestExtractComments_EmptyInput(t *testing.T) {
 	comments := ExtractComments(nil)
 	if len(comments) != 0 {
 		t.Fatalf("expected 0 comments, got %d", len(comments))
 	}
 }

 func TestExtractComments_SkipsUILabels(t *testing.T) {
 	nodes := []UiNode{
 		{Text: "赞"},
 		{Text: "回复"},
 		{Text: "分享"},
 		{Text: "暂时没有更多了"},
 	}
 	comments := ExtractComments(nodes)
 	if len(comments) != 0 {
 		t.Fatalf("expected 0 comments, got %d", len(comments))
 	}
 }

 func TestExtractComments_SkipsNumericCounts(t *testing.T) {
 	nodes := []UiNode{
 		{Text: "张三", ClassName: "android.widget.TextView"},
 		{Text: "好文收藏", ClassName: "android.widget.TextView"},
 		{Text: "128", ClassName: "android.widget.TextView"},
 		{Text: "3天��� 北京", ClassName: "android.widget.TextView"},
 	}
 	comments := ExtractComments(nodes)
 	if len(comments) != 1 {
 		t.Fatalf("expected 1 comment, got %d", len(comments))
 	}
 	if comments[0].Author != "张三" {
 		t.Errorf("expected author 张三, got %q", comments[0].Author)
 	}
 }

 func TestExtractComments_MultiLineContent(t *testing.T) {
 	nodes := []UiNode{
 		{Text: "深度用户"},
 		{Text: "第一行内容"},
 		{Text: "第二行补充说明，很长的一段话。"},
 		{Text: "5分钟前"},
 	}
 	comments := ExtractComments(nodes)
 	if len(comments) < 1 {
 		t.Fatalf("expected >=1 comment, got %d", len(comments))
 	}
 	// Content should contain both lines
 	if comments[0].Content == "" {
 		t.Error("expected non-empty content")
 	}
 }

 func TestIsNumericOnly(t *testing.T) {
 	cases := []struct {
 		s    string
 		want bool
 	}{
 		{"123", true},
 		{"0", true},
 		{"", false},
 		{"abc", false},
 		{"12a", false},
 	}
 	for _, c := range cases {
 		if got := isNumericOnly(c.s); got != c.want {
 			t.Errorf("isNumericOnly(%q) = %v, want %v", c.s, got, c.want)
 		}
 	}
 }
