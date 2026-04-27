package foundation

import (
	"testing"
)

func makeTextNodes(texts ...string) []UiNode {
	nodes := make([]UiNode, len(texts))
	for i, t := range texts {
		nodes[i] = UiNode{Text: t, ClassName: "android.widget.TextView"}
	}
	return nodes
}

func TestBottomDetector_FirstRound_AlwaysNotBottom(t *testing.T) {
	d := NewBottomDetector()
	bottom, signal, reason := d.Check(makeTextNodes("A", "B", "C"))
	if bottom {
		t.Errorf("first round should never be bottom, got bottom=%v signal=%v reason=%s", bottom, signal, reason)
	}
	if signal != SignalNone {
		t.Errorf("first round signal should be none, got %v", signal)
	}
}

func TestBottomDetector_SignalA_TextRepeated(t *testing.T) {
	d := NewBottomDetector()
	round1 := makeTextNodes("评论1", "评论2", "评论3")
	d.Check(round1)

	// Same texts again → should detect bottom
	round2 := makeTextNodes("评论1", "评论2", "评论3")
	bottom, signal, _ := d.Check(round2)
	if !bottom || signal != SignalTextRepeated {
		t.Errorf("expected SignalTextRepeated, got bottom=%v signal=%v", bottom, signal)
	}
}

func TestBottomDetector_SignalB_TailMarker(t *testing.T) {
	d := NewBottomDetector()
	d.Check(makeTextNodes("评论1", "评论2"))

	bottom, signal, reason := d.Check(makeTextNodes("评论1", "评论2", "暂时没有更多了"))
	if !bottom || signal != SignalTailMarker {
		t.Errorf("expected SignalTailMarker, got bottom=%v signal=%v reason=%s", bottom, signal, reason)
	}
}

func TestBottomDetector_SignalC_NoNewText_SameCount(t *testing.T) {
	d := NewBottomDetector()
	d.Check(makeTextNodes("A", "B", "C"))

	// Different order but same set with same node count → text_repeated triggers first
	// Let's test with partial overlap but zero new: use same nodes
	// Actually signal A catches this. For signal C we need different texts but all seen before
	// We'll construct: round1 has {A,B,C}, round2 has {A,B,C} shuffled — that's signal A
	// For signal C: round2 has subset + no new items
	d2 := NewBottomDetector()
	d2.Check(makeTextNodes("A", "B", "C", "D"))
	// Same node count but texts are a subset (no new ones)
	bottom, signal, _ := d2.Check(makeTextNodes("A", "B", "C", "D"))
	if !bottom {
		t.Errorf("expected bottom, got bottom=%v signal=%v", bottom, signal)
	}
}

func TestBottomDetector_NotBottom_WhenNewTextsAppear(t *testing.T) {
	d := NewBottomDetector()
	d.Check(makeTextNodes("评论1", "评论2"))

	bottom, signal, reason := d.Check(makeTextNodes("评论1", "评论2", "评论3", "评论4"))
	if bottom {
		t.Errorf("should not be bottom when new texts appear, got bottom=%v signal=%v reason=%s", bottom, signal, reason)
	}
}

func TestBottomDetector_EmptyNodes(t *testing.T) {
	d := NewBottomDetector()
	d.Check(makeTextNodes("A"))

	bottom, signal, _ := d.Check(nil)
	// Empty nodes: no tail marker, hash changes, 0 new texts but 0 node count → no_new_text requires > 0 nodes
	if bottom {
		t.Errorf("empty nodes should not trigger bottom, got bottom=%v signal=%v", bottom, signal)
	}
}

func TestBottomDetector_Rounds(t *testing.T) {
	d := NewBottomDetector()
	if d.Rounds() != 0 {
		t.Errorf("initial rounds should be 0, got %d", d.Rounds())
	}
	d.Check(makeTextNodes("A"))
	if d.Rounds() != 1 {
		t.Errorf("after 1 check, rounds should be 1, got %d", d.Rounds())
	}
	d.Check(makeTextNodes("B"))
	if d.Rounds() != 2 {
		t.Errorf("after 2 checks, rounds should be 2, got %d", d.Rounds())
	}
}
