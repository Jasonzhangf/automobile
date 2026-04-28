package blocks

import (
	"fmt"
	"strings"
	"time"

	"flowy/services/mac-daemon/src/foundation"
	"flowy/services/mac-daemon/src/state"
)

// ToggleAction describes a toggle operation (like / collect / follow).
type ToggleAction struct {
	// ActionName is the human label (like, collect, follow).
	ActionName string `json:"actionName"`
	// ToggledDesc is the descContains / textContains value when the action is ALREADY done.
	// e.g. "已点赞", "已收藏", "已关注".
	ToggledDesc string `json:"toggledDesc"`
	// ToggledUseText when true matches text instead of contentDescription.
	ToggledUseText bool `json:"toggledUseText,omitempty"`
	// UntoggledDesc is the descContains / textContains value when NOT done.
	// e.g. "点赞", "收藏", "关注".
	UntoggledDesc string `json:"untoggledDesc"`
	// UntoggledUseText when true matches text instead of contentDescription.
	UntoggledUseText bool `json:"untoggledUseText,omitempty"`
}

// ToggleResult holds the outcome of a toggle operation.
type ToggleResult struct {
	ActionName string `json:"actionName"`
	Before     bool   `json:"before"`  // true = was already toggled
	After      bool   `json:"after"`   // true = now toggled
	Toggled    bool   `json:"toggled"` // final state matches desired
}

// ToggleActionBlock observes the page, determines current toggle state,
// taps to reach the desired state, and verifies the result.
// desired=true means "ensure toggled on"; desired=false means "ensure toggled off".
func ToggleActionBlock(
	session *state.ClientSession,
	app *state.AppState,
	artifactRoot string,
	action ToggleAction,
	desired bool,
	backend OperationBackend,
	timeoutMs int,
) (*ToggleResult, error) {
	// 1. Observe current state.
	isToggled, err := detectToggleState(session, app, artifactRoot, action, timeoutMs)
	if err != nil {
		return nil, fmt.Errorf("toggle detect: %w", err)
	}

	result := &ToggleResult{
		ActionName: action.ActionName,
		Before:     isToggled,
	}

	// 2. If already in desired state, nothing to do.
	if isToggled == desired {
		result.After = isToggled
		result.Toggled = true
		return result, nil
	}

	// 3. Tap the button to toggle.
	if err := tapToggleButton(session, app, artifactRoot, action, timeoutMs, backend); err != nil {
		return nil, fmt.Errorf("toggle tap: %w", err)
	}
	time.Sleep(foundation.OperationDelay()) // jitter after tap

	// 4. Verify new state.
	newState, err := detectToggleState(session, app, artifactRoot, action, timeoutMs)
	if err != nil {
		return nil, fmt.Errorf("toggle verify: %w", err)
	}

	result.After = newState
	result.Toggled = (newState == desired)
	if !result.Toggled {
		return result, fmt.Errorf("toggle verify failed: wanted %v, got %v", desired, newState)
	}
	return result, nil
}

// detectToggleState observes the page and checks whether the toggled indicator exists.
func detectToggleState(
	session *state.ClientSession,
	app *state.AppState,
	artifactRoot string,
	action ToggleAction,
	timeoutMs int,
) (bool, error) {
	obs, err := ObservePage(session, app, artifactRoot, timeoutMs)
	if err != nil {
		return false, err
	}
	// Search for the toggled indicator (e.g. "已点赞").
	for _, n := range obs.Nodes {
		if action.ToggledUseText {
			if strings.Contains(n.Text, action.ToggledDesc) {
				return true, nil
			}
		} else {
			if strings.Contains(n.ContentDescription, action.ToggledDesc) {
				return true, nil
			}
		}
	}
	return false, nil
}

// tapToggleButton finds and taps the toggle button.
// Strategy: find the node matching the untoggled or toggled desc/text,
// then tap its center.
func tapToggleButton(
	session *state.ClientSession,
	app *state.AppState,
	artifactRoot string,
	action ToggleAction,
	timeoutMs int,
	backend OperationBackend,
) error {
	obs, err := ObservePage(session, app, artifactRoot, timeoutMs)
	if err != nil {
		return err
	}
	// Try to find the button. It might have either toggled or untoggled text.
	// Look for a node whose className contains "Button" and desc/text contains
	// either the toggled or untoggled prefix.
	prefixes := []string{action.UntoggledDesc, action.ToggledDesc}
	useText := action.UntoggledUseText || action.ToggledUseText

	for _, n := range obs.Nodes {
		if n.BoundsInScreen == nil {
			continue
		}
		match := false
		if useText {
			for _, p := range prefixes {
				if strings.Contains(n.Text, p) {
					match = true
					break
				}
			}
		} else {
			for _, p := range prefixes {
				if strings.Contains(n.ContentDescription, p) {
					match = true
					break
				}
			}
		}
		if match {
			cx, cy := n.BoundsInScreen.Center()
			return TapBlock(session, app, artifactRoot,
				TapTarget{X: &cx, Y: &cy}, backend, timeoutMs)
		}
	}
	return fmt.Errorf("toggle button not found for %s", action.ActionName)
}
