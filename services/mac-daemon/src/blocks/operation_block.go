package blocks

import (
	"fmt"
	"time"

	"flowy/services/mac-daemon/src/foundation"
	"flowy/services/mac-daemon/src/proto"
	"flowy/services/mac-daemon/src/state"
)

// OperationBackend selects the input backend on Android.
type OperationBackend string

const (
	BackendAccessibility OperationBackend = "accessibility"
	BackendRoot          OperationBackend = "root"
)

// TapTarget describes how to resolve the tap point.
type TapTarget struct {
	// Direct point (highest priority).
	X *int `json:"x,omitempty"`
	Y *int `json:"y,omitempty"`
	// Selector-based (uses filter + ObservePage to resolve).
	DescContains string `json:"descContains,omitempty"`
	TextContains string `json:"textContains,omitempty"`
}

// TapBlock sends a tap command to the Android device.
func TapBlock(
	session *state.ClientSession,
	app *state.AppState,
	artifactRoot string,
	target TapTarget,
	backend OperationBackend,
	timeoutMs int,
) error {
	x, y, err := resolveTapPoint(session, app, artifactRoot, target, timeoutMs)
	if err != nil {
		return fmt.Errorf("tap: resolve point failed: %w", err)
	}
	cmd := proto.CommandEnvelope{
		ProtocolVersion: "exp01",
		RequestID:       foundation.NewRequestID(time.Now(), app.NextRequestSeq()),
		RunID:           foundation.NewRunID(time.Now(), "tap", "operation"),
		Command:         "tap",
		SentAt:          time.Now().Format(time.RFC3339),
		TimeoutMs:       timeoutMs,
		Payload: map[string]any{
			"x":       x,
			"y":       y,
			"backend": string(backend),
		},
	}
	resp, err := CommandRoundtrip(session, app, cmd)
	if err != nil {
		return fmt.Errorf("tap command failed: %w", err)
	}
	if resp.Status != "ok" {
		msg := ""
		if resp.Error != nil {
			msg = resp.Error.Message
		}
		return fmt.Errorf("tap returned error: %s", msg)
	}
	return nil
}

// ScrollTarget describes where to scroll.
type ScrollTarget struct {
	// Direct point.
	X *int `json:"x,omitempty"`
	Y *int `json:"y,omitempty"`
	// Selector-based.
	DescContains string `json:"descContains,omitempty"`
	TextContains string `json:"textContains,omitempty"`
	// Defaults to a scrollable node if empty.
}

// ScrollBlock sends a scroll command to the Android device.
func ScrollBlock(
	session *state.ClientSession,
	app *state.AppState,
	artifactRoot string,
	target ScrollTarget,
	direction string,
	backend OperationBackend,
	timeoutMs int,
) error {
	if direction == "" {
		direction = "forward"
	}
	x, y, err := resolveScrollPoint(session, app, artifactRoot, target, timeoutMs)
	if err != nil {
		return fmt.Errorf("scroll: resolve point failed: %w", err)
	}
	cmd := proto.CommandEnvelope{
		ProtocolVersion: "exp01",
		RequestID:       foundation.NewRequestID(time.Now(), app.NextRequestSeq()),
		RunID:           foundation.NewRunID(time.Now(), "scroll", "operation"),
		Command:         "scroll",
		SentAt:          time.Now().Format(time.RFC3339),
		TimeoutMs:       timeoutMs,
		Payload: map[string]any{
			"x":         x,
			"y":         y,
			"direction":  direction,
			"backend":   string(backend),
		},
	}
	resp, err := CommandRoundtrip(session, app, cmd)
	if err != nil {
		return fmt.Errorf("scroll command failed: %w", err)
	}
	if resp.Status != "ok" {
		msg := ""
		if resp.Error != nil {
			msg = resp.Error.Message
		}
		return fmt.Errorf("scroll returned error: %s", msg)
	}
	return nil
}

// BackBlock sends a back (press key 4) command to the Android device.
func BackBlock(
	session *state.ClientSession,
	app *state.AppState,
	backend OperationBackend,
	timeoutMs int,
) error {
	cmd := proto.CommandEnvelope{
		ProtocolVersion: "exp01",
		RequestID:       foundation.NewRequestID(time.Now(), app.NextRequestSeq()),
		RunID:           foundation.NewRunID(time.Now(), "press-key", "operation"),
		Command:         "press-key",
		SentAt:          time.Now().Format(time.RFC3339),
		TimeoutMs:       timeoutMs,
		Payload: map[string]any{
			"keyCode": 4,
			"backend": string(backend),
		},
	}
	resp, err := CommandRoundtrip(session, app, cmd)
	if err != nil {
		return fmt.Errorf("back command failed: %w", err)
	}
	if resp.Status != "ok" {
		msg := ""
		if resp.Error != nil {
			msg = resp.Error.Message
		}
		return fmt.Errorf("back returned error: %s", msg)
	}
	return nil
}

// InputTextBlock sends an input-text command to the Android device.
func InputTextBlock(
	session *state.ClientSession,
	app *state.AppState,
	text string,
	backend OperationBackend,
	timeoutMs int,
) error {
	cmd := proto.CommandEnvelope{
		ProtocolVersion: "exp01",
		RequestID:       foundation.NewRequestID(time.Now(), app.NextRequestSeq()),
		RunID:           foundation.NewRunID(time.Now(), "input-text", "operation"),
		Command:         "input-text",
		SentAt:          time.Now().Format(time.RFC3339),
		TimeoutMs:       timeoutMs,
		Payload: map[string]any{
			"text":    text,
			"backend": string(backend),
		},
	}
	resp, err := CommandRoundtrip(session, app, cmd)
	if err != nil {
		return fmt.Errorf("input-text command failed: %w", err)
	}
	if resp.Status != "ok" {
		msg := ""
		if resp.Error != nil {
			msg = resp.Error.Message
		}
		return fmt.Errorf("input-text returned error: %s", msg)
	}
	return nil
}

// resolveTapPoint resolves direct coordinates or selector to (x, y).
func resolveTapPoint(session *state.ClientSession, app *state.AppState, artifactRoot string, target TapTarget, timeoutMs int) (int, int, error) {
	if target.X != nil && target.Y != nil {
		return *target.X, *target.Y, nil
	}
	obs, err := ObservePage(session, app, artifactRoot, timeoutMs)
	if err != nil {
		return 0, 0, err
	}
	node := findNodeBySelector(obs.Nodes, target.DescContains, target.TextContains)
	if node == nil {
		return 0, 0, fmt.Errorf("no node matching selector")
	}
	if node.BoundsInScreen == nil {
		return 0, 0, fmt.Errorf("target node has no bounds")
	}
	cx, cy := node.BoundsInScreen.Center()
	return cx, cy, nil
}

// resolveScrollPoint resolves direct coordinates or selector to (x, y).
func resolveScrollPoint(session *state.ClientSession, app *state.AppState, artifactRoot string, target ScrollTarget, timeoutMs int) (int, int, error) {
	if target.X != nil && target.Y != nil {
		return *target.X, *target.Y, nil
	}
	obs, err := ObservePage(session, app, artifactRoot, timeoutMs)
	if err != nil {
		return 0, 0, err
	}
	// Try selector first, then fall back to any scrollable node.
	if target.DescContains != "" || target.TextContains != "" {
		node := findNodeBySelector(obs.Nodes, target.DescContains, target.TextContains)
		if node != nil && node.BoundsInScreen != nil {
			cx, cy := node.BoundsInScreen.Center()
			return cx, cy, nil
		}
	}
	// Fall back: find first scrollable node.
 for _, n := range obs.Nodes {
		if n.Flags.Scrollable && n.BoundsInScreen != nil {
			cx, cy := n.BoundsInScreen.Center()
			return cx, cy, nil
		}
	}
	return 0, 0, fmt.Errorf("no scrollable node found")
}

// findNodeBySelector finds the first node matching descContains and/or textContains.
func findNodeBySelector(nodes []foundation.UiNode, descContains, textContains string) *foundation.UiNode {
	for i := range nodes {
		n := &nodes[i]
		descOk := descContains == "" || containsStr(n.ContentDescription, descContains)
		textOk := textContains == "" || containsStr(n.Text, textContains)
		if descOk && textOk && (descContains != "" || textContains != "") {
			return n
		}
	}
	return nil
}

func containsStr(s, substr string) bool {
	return len(substr) > 0 && len(s) >= len(substr) && findSubstr(s, substr)
}

func findSubstr(s, sub string) bool {
	for i := 0; i <= len(s)-len(sub); i++ {
		if s[i:i+len(sub)] == sub {
			return true
		}
	}
	return false
}
