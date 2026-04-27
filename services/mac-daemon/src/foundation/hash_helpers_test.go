package foundation

import (
	"testing"
)

func TestItemIDFromTitle_Deterministic(t *testing.T) {
	id1 := ItemIDFromTitle("DeepSeek V4 接入Claude Code最全教程🔥")
	id2 := ItemIDFromTitle("DeepSeek V4 接入Claude Code最全教程🔥")
	if id1 != id2 {
		t.Fatalf("expected deterministic hash, got %s vs %s", id1, id2)
	}
	if len(id1) != 16 {
		t.Fatalf("expected 16 hex chars, got %d", len(id1))
	}
}

func TestItemIDFromTitle_EmptyInput(t *testing.T) {
	if id := ItemIDFromTitle(""); id != "" {
		t.Fatalf("expected empty for empty title, got %s", id)
	}
	if id := ItemIDFromTitle("   "); id != "" {
		t.Fatalf("expected empty for whitespace-only title, got %s", id)
	}
}

func TestItemIDFromTitle_DifferentTitles(t *testing.T) {
	id1 := ItemIDFromTitle("DeepSeek V4 教程")
	id2 := ItemIDFromTitle("Claude Code 配置")
	if id1 == id2 {
		t.Fatalf("expected different hashes for different titles, both got %s", id1)
	}
}

func TestItemIDFromTitle_WhitespaceNormalized(t *testing.T) {
	// TrimWhitespace should normalize
	id1 := ItemIDFromTitle("  hello world  ")
	id2 := ItemIDFromTitle("hello world")
	if id1 != id2 {
		t.Fatalf("expected same hash after trim, got %s vs %s", id1, id2)
	}
}

func TestItemIDFromComposite_Deterministic(t *testing.T) {
	id1 := ItemIDFromComposite("标题", "作者")
	id2 := ItemIDFromComposite("标题", "作者")
	if id1 != id2 {
		t.Fatalf("expected deterministic, got %s vs %s", id1, id2)
	}
}

func TestItemIDFromComposite_DifferentFromTitle(t *testing.T) {
	idTitle := ItemIDFromTitle("标题")
	idComp := ItemIDFromComposite("标题", "作者")
	if idTitle == idComp {
		t.Fatalf("composite should differ from title-only, both got %s", idTitle)
	}
}

func TestItemIDFromComposite_EmptyInputs(t *testing.T) {
	if id := ItemIDFromComposite("", ""); id != "" {
		t.Fatalf("expected empty for both empty, got %s", id)
	}
}

func TestItemIDWithFallback_TitleFirst(t *testing.T) {
	id := ItemIDWithFallback("有效的标题", "作者")
	idExpected := ItemIDFromTitle("有效的标题")
	if id != idExpected {
		t.Fatalf("expected fallback to use title hash, got %s vs %s", id, idExpected)
	}
}

func TestItemIDWithFallback_CompositeWhenEmpty(t *testing.T) {
	id := ItemIDWithFallback("", "作者")
	idExpected := ItemIDFromComposite("", "作者")
	if id != idExpected {
		t.Fatalf("expected fallback to composite when title empty, got %s vs %s", id, idExpected)
	}
}

func TestNormalizeForHash(t *testing.T) {
	tests := []struct {
		input    string
		expected string
	}{
		{"  hello   world  ", "hello world"},
		{"hello\n\nworld", "hello world"},
		{"hello\t\tworld", "hello world"},
		{"  ", ""},
		{"", ""},
		{"正常标题", "正常标题"},
	}
	for _, tc := range tests {
		got := NormalizeForHash(tc.input)
		if got != tc.expected {
			t.Errorf("NormalizeForHash(%q) = %q, want %q", tc.input, got, tc.expected)
		}
	}
}
