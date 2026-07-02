package blocks

import (
	"fmt"
	"strings"

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
	X            *int   `json:"x,omitempty"`
	Y            *int   `json:"y,omitempty"`
	DescContains string `json:"descContains,omitempty"`
	TextContains string `json:"textContains,omitempty"`
}

// ScrollTarget describes where to scroll.
type ScrollTarget struct {
	X            *int   `json:"x,omitempty"`
	Y            *int   `json:"y,omitempty"`
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
	tr := makeTransport(session, app)
	return tapWithTransport(tr, artifactRoot, target, backend, timeoutMs)
}

func tapWithTransport(tr *Transport, artifactRoot string, target TapTarget, backend OperationBackend, timeoutMs int) error {
	x, y, err := resolveTapPoint(tr, artifactRoot, target, timeoutMs)
	if err != nil {
		return fmt.Errorf("tap: resolve point failed: %w", err)
	}
	cmd := NewCommand(tr, proto.CmdTap, map[string]any{
		"x": x, "y": y, "backend": string(backend),
	})
	if timeoutMs > 0 {
		cmd.TimeoutMs = timeoutMs
	}
	resp, err := transportRoundtrip(tr, cmd)
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
	tr := makeTransport(session, app)
	return scrollWithTransport(tr, artifactRoot, target, direction, backend, timeoutMs)
}

func scrollWithTransport(tr *Transport, artifactRoot string, target ScrollTarget, direction string, backend OperationBackend, timeoutMs int) error {
	if direction == "" {
		direction = "forward"
	}
	x, y, err := resolveScrollPoint(tr, artifactRoot, target, timeoutMs)
	if err != nil {
		return fmt.Errorf("scroll: resolve point failed: %w", err)
	}
	cmd := NewCommand(tr, proto.CmdScroll, map[string]any{
		"x": x, "y": y, "direction": direction, "backend": string(backend),
	})
	if timeoutMs > 0 {
		cmd.TimeoutMs = timeoutMs
	}
	resp, err := transportRoundtrip(tr, cmd)
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
	tr := makeTransport(session, app)
	return backWithTransport(tr, backend, timeoutMs)
}

func backWithTransport(tr *Transport, backend OperationBackend, timeoutMs int) error {
	cmd := NewCommand(tr, proto.CmdPressKey, map[string]any{
		"keyCode": 4, "backend": string(backend),
	})
	if timeoutMs > 0 {
		cmd.TimeoutMs = timeoutMs
	}
	resp, err := transportRoundtrip(tr, cmd)
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
	tr := makeTransport(session, app)
	return inputTextWithTransport(tr, text, backend, timeoutMs)
}

func inputTextWithTransport(tr *Transport, text string, backend OperationBackend, timeoutMs int) error {
	cmd := NewCommand(tr, proto.CmdInputText, map[string]any{
		"text": text, "backend": string(backend),
	})
	if timeoutMs > 0 {
		cmd.TimeoutMs = timeoutMs
	}
	resp, err := transportRoundtrip(tr, cmd)
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

// --- Internal helpers -----------------------------------------------

func resolveTapPoint(tr *Transport, artifactRoot string, target TapTarget, timeoutMs int) (int, int, error) {
	if target.X != nil && target.Y != nil {
		return *target.X, *target.Y, nil
	}
	obs, err := observePageWithTransport(tr, artifactRoot, timeoutMs)
	if err != nil {
		return 0, 0, err
	}
	n := findNodeBySelector(obs.Nodes, target.DescContains, target.TextContains)
	if n == nil || n.BoundsInScreen == nil {
		return 0, 0, fmt.Errorf("tap target not found (desc=%q text=%q)", target.DescContains, target.TextContains)
	}
	cx, cy := n.BoundsInScreen.Center(); return cx, cy, nil
}

func resolveScrollPoint(tr *Transport, artifactRoot string, target ScrollTarget, timeoutMs int) (int, int, error) {
	if target.X != nil && target.Y != nil {
		return *target.X, *target.Y, nil
	}
	obs, err := observePageWithTransport(tr, artifactRoot, timeoutMs)
	if err != nil {
		return 0, 0, err
	}
	n := findNodeBySelector(obs.Nodes, target.DescContains, target.TextContains)
	if n == nil || n.BoundsInScreen == nil {
		// Fallback to center screen (approximate scroll position)
		return 540, 1200, nil
	}
	cx, cy := n.BoundsInScreen.Center(); return cx, cy, nil
}

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
	return s != "" && strings.Contains(s, substr)
}

func findSubstr(s, sub string) bool {
	return s != "" && strings.Contains(s, sub)
}

