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
	ActionName     string `json:"actionName"`
	ToggledDesc    string `json:"toggledDesc"`
	ToggledUseText bool   `json:"toggledUseText,omitempty"`
	UntoggledDesc  string `json:"untoggledDesc"`
	UntoggledUseText bool `json:"untoggledUseText,omitempty"`
}

// ToggleResult holds the outcome of a toggle operation.
type ToggleResult struct {
	ActionName string `json:"actionName"`
	Before     bool   `json:"before"`
	After      bool   `json:"after"`
	Toggled    bool   `json:"toggled"`
}

// ToggleActionBlock observes the page, determines current toggle state,
// taps to reach the desired state, and verifies the result.
func ToggleActionBlock(
	session *state.ClientSession,
	app *state.AppState,
	artifactRoot string,
	action ToggleAction,
	desired bool,
	backend OperationBackend,
	timeoutMs int,
) (*ToggleResult, error) {
	tr := MakeTransport(session, app)
	return toggleActionBlockWithTransport(tr, artifactRoot, action, desired, backend, timeoutMs)
}

func toggleActionBlockWithTransport(
	tr *Transport,
	artifactRoot string,
	action ToggleAction,
	desired bool,
	backend OperationBackend,
	timeoutMs int,
) (*ToggleResult, error) {
	isToggled, err := detectToggleStateWithTransport(tr, artifactRoot, action, timeoutMs)
	if err != nil {
		return nil, fmt.Errorf("toggle detect: %w", err)
	}

	result := &ToggleResult{
		ActionName: action.ActionName,
		Before:     isToggled,
	}

	if isToggled == desired {
		result.After = isToggled
		result.Toggled = true
		return result, nil
	}

	if err := tapToggleButtonWithTransport(tr, artifactRoot, action, timeoutMs, backend); err != nil {
		return nil, fmt.Errorf("toggle tap: %w", err)
	}
	time.Sleep(foundation.OperationDelay())

	newState, err := detectToggleStateWithTransport(tr, artifactRoot, action, timeoutMs)
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

func detectToggleStateWithTransport(
	tr *Transport,
	artifactRoot string,
	action ToggleAction,
	timeoutMs int,
) (bool, error) {
	obs, err := observePageWithTransport(tr, artifactRoot, timeoutMs)
	if err != nil {
		return false, err
	}
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

func tapToggleButtonWithTransport(
	tr *Transport,
	artifactRoot string,
	action ToggleAction,
	timeoutMs int,
	backend OperationBackend,
) error {
	obs, err := observePageWithTransport(tr, artifactRoot, timeoutMs)
	if err != nil {
		return err
	}
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
			return tapWithTransport(tr, artifactRoot,
				TapTarget{X: &cx, Y: &cy}, backend, timeoutMs)
		}
	}
	return fmt.Errorf("toggle button not found for %s", action.ActionName)
}
