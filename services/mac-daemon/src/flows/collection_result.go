package flows

import (
	"time"
)

// skipCurrentItem records a skip result and transitions back to list state.
func (fc *FlowContext) skipCurrentItem(reason string) {
	if fc.CurrentItem != nil {
		fc.Results = append(fc.Results, CollectionResult{
			ItemID:      fc.itemIDFromMatch(*fc.CurrentItem),
			CollectedAt: time.Now().Format(time.RFC3339),
			Fields:      map[string]string{"_skip_reason": reason},
		})
	}
	fc.CurrentItem = nil
	fc.DetailRetries = 0
	fc.State = StateBackToList
}

// buildResult aggregates collection results into a run result.
func (fc *FlowContext) buildResult(status string) CollectionRunResult {
	success, skipped, failed := 0, 0, 0
	for _, r := range fc.Results {
		if _, hasSkip := r.Fields["_skip_reason"]; hasSkip {
			skipped++
		} else {
			success++
		}
	}
	errMsg := ""
	if fc.Error != nil {
		errMsg = fc.Error.Error()
		failed = 1
	}
	return CollectionRunResult{
		Status:       status,
		TotalItems:   len(fc.Results),
		SuccessItems: success,
		SkippedItems: skipped,
		FailedItems:  failed,
		Items:        fc.Results,
		Error:        errMsg,
	}
}
