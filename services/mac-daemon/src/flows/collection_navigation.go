package flows

import (
	"flowy/services/mac-daemon/src/blocks"
	"flowy/services/mac-daemon/src/foundation"
)

// sendCommandToDevice sends a command via WS with auto-injected request/run IDs.
func (fc *FlowContext) sendCommandToDevice(command string, payload map[string]any) {
	tr := blocks.MakeTransport(fc.Session, fc.App)
	cmd := blocks.NewCommand(tr, command, payload)
	blocks.CommandRoundtripWithTransport(tr, cmd)
}

// itemIDFromMatch extracts a dedup item ID from a match result.
func (fc *FlowContext) itemIDFromMatch(m foundation.MatchResult) string {
	text := m.Node.Text
	if text == "" {
		text = m.Node.ContentDescription
	}
	return foundation.ItemIDFromTitle(text)
}

// anchorCheck verifies a named anchor on the current page.
func (fc *FlowContext) anchorCheck(spec foundation.AnchorSpec, label string) bool {
	result, err := blocks.AnchorBlock(fc.Session, fc.App, fc.ArtifactRoot,
		spec, blocks.DefaultAnchorConfig())
	if err != nil || !result.Matched {
		return false
	}
	return true
}

// Stop signals the flow to stop gracefully after the current iteration.
func (fc *FlowContext) Stop() {
	if !fc.stopped {
		close(fc.stopCh)
	}
}

// IsActive returns true if the flow is still running.
func (fc *FlowContext) IsActive() bool {
	return !fc.stopped
}
