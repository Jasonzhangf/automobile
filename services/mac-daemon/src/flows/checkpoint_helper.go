package flows

import (
	"flowy/services/mac-daemon/src/foundation"
)

// WriteFlowCheckpoint writes a checkpoint capturing the current flow state.
// Called on each major state transition for crash-resume support.
func WriteFlowCheckpoint(checkpointDir string, fc *FlowContext) {
	cp := &foundation.Checkpoint{
		RunID:        "",
		CurrentState: string(fc.State),
		EntryContext: foundation.EntryContext{
			Mode:    string(fc.Config.Mode),
			Keyword: fc.Config.Keyword,
			App:     fc.Profile.AppID,
		},
		ListPosition: foundation.ListPosition{
			ScrollIndex: fc.ScrollRetries,
		},
		CompletedCount: len(fc.Results),
		TargetCount:    fc.Config.TargetCount,
		ErrorCount:     countErrors(fc.Results),
	}
	if fc.CurrentItem != nil {
		cp.CurrentItemID = fc.computeItemID(*fc.CurrentItem)
	}
	// Collect completed IDs
	for _, r := range fc.Results {
		cp.CompletedIDs = append(cp.CompletedIDs, r.ItemID)
	}
	foundation.WriteCheckpoint(checkpointDir, cp)
}

// LoadFlowCheckpoint reads a checkpoint and applies it to the flow context for resume.
func LoadFlowCheckpoint(checkpointDir string, fc *FlowContext) bool {
	cp, err := foundation.ReadCheckpoint(checkpointDir)
	if err != nil || cp == nil {
		return false
	}
	// Restore target count and completed items
	fc.Config.TargetCount = cp.TargetCount
	fc.ScrollRetries = cp.ListPosition.ScrollIndex
	// Restore state
	fc.State = FlowState(cp.CurrentState)
	// Skip init since we're resuming directly
	if fc.State == StateInit {
		fc.State = StatePickNext
	}
	return true
}

func countErrors(results []CollectionResult) int {
	n := 0
	for _, r := range results {
		if _, ok := r.Fields["_skip_reason"]; ok {
			n++
		}
	}
	return n
}
