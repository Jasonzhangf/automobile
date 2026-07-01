package flows

import (
	"fmt"
	"time"

	"flowy/services/mac-daemon/src/blocks"
	"flowy/services/mac-daemon/src/foundation"
	"flowy/services/mac-daemon/src/state"
)

// --- State machine states -----------------------------------------------

type FlowState string

const (
	StateInit          FlowState = "INIT"
	StateListEntry     FlowState = "LIST_ENTRY"
	StatePickNext      FlowState = "PICK_NEXT"
	StateEnterDetail   FlowState = "ENTER_DETAIL"
	StateDetailTask    FlowState = "DETAIL_TASK"
	StateBackToList    FlowState = "BACK_TO_LIST"
	StateCheckContinue FlowState = "CHECK_CONTINUE"
	StateDone          FlowState = "DONE"
	StateError         FlowState = "ERROR"
)

// --- Flow context -------------------------------------------------------

type FlowContext struct {
	Session       *state.ClientSession
	App           *state.AppState
	ArtifactRoot  string
	Config        CollectionConfig
	Profile       CollectionProfile
	Dedup         *foundation.DedupStore
	State         FlowState
	Results       []CollectionResult
	CurrentItem   *foundation.MatchResult
	ScrollRetries int
	DetailRetries int
	Error         error
	// Timeouts
	runDeadline    time.Time // zero = no run timeout
	detailDeadline time.Time // zero = no detail timeout
	detailStart    time.Time // when current detail phase started
	// Graceful stop
	stopCh chan struct{}
	stopped bool
}

// NewFlowContext creates a new flow context.
func NewFlowContext(
	session *state.ClientSession,
	app *state.AppState,
	artifactRoot string,
	config CollectionConfig,
	profile CollectionProfile,
	dedupStorePath string,
) (*FlowContext, error) {
	dedup, err := foundation.LoadDedupStore(dedupStorePath)
	if err != nil {
		return nil, fmt.Errorf("init dedup store: %w", err)
	}
	return &FlowContext{
		Session:      session,
		App:          app,
		ArtifactRoot: artifactRoot,
		Config:       config,
		Profile:      profile,
		Dedup:        dedup,
		State:        StateInit,
		Results:      []CollectionResult{},
		stopCh:       make(chan struct{}),
	}, nil
}

// --- Main loop -----------------------------------------------------------

// Run executes the full collection flow state machine.
func (fc *FlowContext) Run() CollectionRunResult {
	// Allow external stop
	defer func() { fc.stopped = true }()

	backend := blocks.OperationBackend(fc.Config.Backend)
	if backend == "" {
		backend = blocks.BackendRoot
	}

	// Set up timeouts
	if fc.Config.DetailTimeoutMs <= 0 {
		fc.Config.DetailTimeoutMs = 120000 // 120s default
	}
	if fc.Config.RunTimeoutMs > 0 {
		fc.runDeadline = time.Now().Add(time.Duration(fc.Config.RunTimeoutMs) * time.Millisecond)
	}

	maxIters := fc.Config.TargetCount*5 + 20 // safety bound
	for i := 0; i < maxIters; i++ {
		// Check graceful stop
		select {
		case <-fc.stopCh:
			fc.Error = fmt.Errorf("stopped by user")
			return fc.buildResult("stopped")
		default:
		}

		// Check run timeout
		if !fc.runDeadline.IsZero() && time.Now().After(fc.runDeadline) {
			fc.Error = fmt.Errorf("run timeout after %dms", fc.Config.RunTimeoutMs)
			return fc.buildResult("aborted")
		}
		// Check per-detail timeout
		if fc.State == StateDetailTask || fc.State == StateEnterDetail || fc.State == StateBackToList {
			if !fc.detailDeadline.IsZero() && time.Now().After(fc.detailDeadline) {
				fc.skipCurrentItem("detail_timeout")
				continue
			}
		}
		switch fc.State {
		case StateInit:
			fc.stateInit()
		case StateListEntry:
			fc.stateListEntry(backend)
		case StatePickNext:
			fc.statePickNext()
		case StateEnterDetail:
			fc.stateEnterDetail(backend)
		case StateDetailTask:
			fc.stateDetailTask(backend)
		case StateBackToList:
			fc.stateBackToList(backend)
		case StateCheckContinue:
			fc.stateCheckContinue()
		case StateDone:
			return fc.buildResult("completed")
		case StateError:
			return fc.buildResult("aborted")
		}
	}
	fc.Error = fmt.Errorf("exceeded max iterations")
	return fc.buildResult("aborted")
}

// --- State implementations -----------------------------------------------

func (fc *FlowContext) stateInit() {
	if fc.Config.TargetCount <= 0 {
		fc.Config.TargetCount = 10
	}
	fc.State = StateListEntry
}

func (fc *FlowContext) stateListEntry(backend blocks.OperationBackend) {
	if fc.Config.Mode == ModeSearch {
		fc.stateListEntrySearch(backend)
	} else {
		fc.stateListEntryTimeline(backend)
	}
}

func (fc *FlowContext) stateListEntrySearch(backend blocks.OperationBackend) {
	// 1. Launch app via intent
	fc.sendCommandToDevice("open-deep-link", map[string]any{"packageName": fc.Profile.PackageName})
	time.Sleep(foundation.OperationDelay())

	// 2. Pre-anchor: verify we're on the main page
	if !fc.anchorCheck(fc.Profile.ListAnchor, "list_entry_pre") {
		fc.State = StateError
		fc.Error = fmt.Errorf("list entry pre-anchor failed")
		return
	}

	// 3. Tap search entry (find by desc/text from profile or convention)
	if err := blocks.TapBlock(fc.Session, fc.App, fc.ArtifactRoot,
		blocks.TapTarget{DescContains: "搜索"},
		backend, 10000); err != nil {
		// try text-based
		if err2 := blocks.TapBlock(fc.Session, fc.App, fc.ArtifactRoot,
			blocks.TapTarget{TextContains: "搜索"},
			backend, 10000); err2 != nil {
			fc.State = StateError
			fc.Error = fmt.Errorf("tap search entry: %w / %w", err, err2)
			return
		}
	}
	time.Sleep(foundation.OperationDelay())

	// 4. Input keyword
	if err := blocks.InputTextBlock(fc.Session, fc.App, fc.Config.Keyword, backend, 10000); err != nil {
		fc.State = StateError
		fc.Error = fmt.Errorf("input keyword: %w", err)
		return
	}
	time.Sleep(foundation.OperationDelay())

	// 5. Press enter
	fc.sendCommandToDevice("press-key", map[string]any{"keyCode": 66, "backend": backend})
	time.Sleep(foundation.OperationDelay())

	// 6. Post-anchor: verify we're on search results
	if !fc.anchorCheck(fc.Profile.ListAnchor, "list_entry_post") {
		fc.State = StateError
		fc.Error = fmt.Errorf("search results anchor failed")
		return
	}

	fc.State = StatePickNext
}

func (fc *FlowContext) stateListEntryTimeline(backend blocks.OperationBackend) {
	fc.sendCommandToDevice("open-deep-link", map[string]any{"packageName": fc.Profile.PackageName})
	time.Sleep(foundation.OperationDelay())

	if !fc.anchorCheck(fc.Profile.ListAnchor, "list_entry_timeline") {
		fc.State = StateError
		fc.Error = fmt.Errorf("timeline anchor failed")
		return
	}

	fc.State = StatePickNext
}

func (fc *FlowContext) statePickNext() {
	obs, err := blocks.ObservePage(fc.Session, fc.App, fc.ArtifactRoot, 10000)
	if err != nil {
		fc.State = StateError
		fc.Error = fmt.Errorf("observe for pick_next: %w", err)
		return
	}

	matches := foundation.MatchNodes(obs.Nodes, fc.Profile.ItemFilter)
	selected, _ := foundation.SelectTargets(matches, "top_most")

	for _, m := range selected {
		itemID := fc.itemIDFromMatch(m)
		if !fc.Dedup.Contains(itemID) {
			fc.CurrentItem = &m
			fc.State = StateEnterDetail
			return
		}
	}

	// No unseen item found — scroll and retry
	fc.ScrollRetries++
	if fc.ScrollRetries > 3 {
		fc.State = StateCheckContinue
		fc.ScrollRetries = 0
		return
	}

	// Scroll forward
	blocks.ScrollBlock(fc.Session, fc.App, fc.ArtifactRoot,
		blocks.ScrollTarget{}, "forward", blocks.BackendRoot, 10000)
	time.Sleep(foundation.OperationDelay())
	fc.State = StatePickNext // re-enter to look again
}

func (fc *FlowContext) stateEnterDetail(backend blocks.OperationBackend) {
	if fc.CurrentItem == nil || fc.CurrentItem.Node.BoundsInScreen == nil {
		fc.State = StatePickNext
		return
	}

	// Start detail timeout tracking
	fc.detailStart = time.Now()
	fc.detailDeadline = time.Now().Add(time.Duration(fc.Config.DetailTimeoutMs) * time.Millisecond)

	// Pre-anchor: verify still on list
	// (skipped here to avoid excessive observe; the fact we just picked means we're on list)

	// Tap the item center
	cx, cy := fc.CurrentItem.Node.BoundsInScreen.Center()
	if err := blocks.TapBlock(fc.Session, fc.App, fc.ArtifactRoot,
		blocks.TapTarget{X: &cx, Y: &cy},
		backend, 10000); err != nil {
		fc.DetailRetries++
		if fc.DetailRetries > 3 {
			fc.skipCurrentItem("tap_failed")
			return
		}
		time.Sleep(foundation.OperationDelay())
		fc.State = StateEnterDetail
		return
	}
	time.Sleep(foundation.OperationDelay())

	// Post-anchor: verify entered detail
	if !fc.anchorCheck(fc.Profile.DetailAnchor, "enter_detail_post") {
		fc.DetailRetries++
		if fc.DetailRetries > 3 {
			fc.skipCurrentItem("detail_anchor_failed")
			return
		}
		time.Sleep(foundation.OperationDelay())
		fc.State = StateBackToList
		return
	}

	fc.DetailRetries = 0
	fc.State = StateDetailTask
}

func (fc *FlowContext) stateDetailTask(backend blocks.OperationBackend) {
	// 1. Observe detail page
	obs, err := blocks.ObservePage(fc.Session, fc.App, fc.ArtifactRoot, 10000)
	if err != nil {
		fc.skipCurrentItem("observe_detail_failed")
		return
	}

	// 2. Extract fields
	result := CollectionResult{
		ItemID:      fc.itemIDFromMatch(*fc.CurrentItem),
		CollectedAt: time.Now().Format(time.RFC3339),
		Fields:      make(map[string]string),
	}

	for _, field := range fc.Profile.DetailFields {
		value := fc.extractField(obs.Nodes, field)
		if value != "" {
			result.Fields[field.Name] = value
		}
	}

	// Try to extract title from first meaningful text
	if result.Title == "" && fc.CurrentItem.Node.Text != "" {
		result.Title = fc.CurrentItem.Node.Text
	}

	// 3. Mark as seen
	fc.Dedup.Append(result.ItemID, result.Title, "")
	fc.Results = append(fc.Results, result)

	fc.State = StateBackToList
}

func (fc *FlowContext) stateBackToList(backend blocks.OperationBackend) {
	if err := blocks.BackBlock(fc.Session, fc.App, backend, 10000); err != nil {
		// back failed — try restarting app
		fc.sendCommandToDevice("open-deep-link", map[string]any{"packageName": fc.Profile.PackageName})
		time.Sleep(foundation.OperationDelay())
		fc.State = StateCheckContinue
		return
	}
	time.Sleep(foundation.OperationDelay())

	// Post-anchor: verify back on list
	if !fc.anchorCheck(fc.Profile.ListAnchor, "back_to_list") {
		// try one more back
		blocks.BackBlock(fc.Session, fc.App, backend, 10000)
		time.Sleep(foundation.OperationDelay())
	}

	fc.CurrentItem = nil
	fc.State = StateCheckContinue
}

func (fc *FlowContext) stateCheckContinue() {
	if len(fc.Results) >= fc.Config.TargetCount {
		fc.State = StateDone
		return
	}
	fc.State = StatePickNext
	fc.ScrollRetries = 0
}

// --- Helpers -------------------------------------------------------------

