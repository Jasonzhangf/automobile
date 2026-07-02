package blocks

import (
	"encoding/json"
	"fmt"
	"os"
	"path/filepath"

	"flowy/services/mac-daemon/src/foundation"
	"flowy/services/mac-daemon/src/proto"
	"flowy/services/mac-daemon/src/state"
)

// ObserveResult contains the parsed UI tree and metadata from a page observation.
type ObserveResult struct {
	RunDir      string              `json:"-"`
	RunID       string              `json:"runId"`
	RequestID   string              `json:"requestId"`
	PackageName string              `json:"packageName"`
	Nodes       []foundation.UiNode `json:"nodes"`
	NodeCount   int                 `json:"nodeCount"`
}

// ObservePage sends a dump-ui-tree-root command to the Android device,
// waits for the response, then reads and parses the artifact.
func ObservePage(session *state.ClientSession, app *state.AppState, artifactRoot string, timeoutMs int) (*ObserveResult, error) {
	tr := makeTransport(session, app)
	return observePageWithTransport(tr, artifactRoot, timeoutMs)
}

// observePageWithTransport is the Transport-aware implementation of ObservePage.
func observePageWithTransport(tr *Transport, artifactRoot string, timeoutMs int) (*ObserveResult, error) {
	cmd := NewCommand(tr, proto.CmdDumpUiTreeRoot, map[string]any{})
	if timeoutMs > 0 {
		cmd.TimeoutMs = timeoutMs
	}

	response, err := transportRoundtrip(tr, cmd)
	if err != nil {
		return nil, fmt.Errorf("observe page command failed: %w", err)
	}
	if response.Status != "ok" {
		errMsg := ""
		if response.Error != nil {
			errMsg = response.Error.Message
		}
		return nil, fmt.Errorf("observe page returned error: %s", errMsg)
	}

	// Read the artifact from the first artifact file
	if len(response.Artifacts) == 0 {
		return nil, fmt.Errorf("observe page: no artifacts in response")
	}
	artifact := response.Artifacts[0]
	runDir := foundation.RunDirFromRunID(artifactRoot, response.RunID)
	artifactPath := filepath.Join(runDir, artifact.FileName)
	data, err := os.ReadFile(artifactPath)
	if err != nil {
		return nil, fmt.Errorf("observe page: read artifact: %w", err)
	}

	var observeData struct {
		PackageName string              `json:"packageName"`
		Nodes       []foundation.UiNode `json:"nodes"`
	}
	if err := json.Unmarshal(data, &observeData); err != nil {
		return nil, fmt.Errorf("observe page: parse artifact: %w", err)
	}
	if observeData.Nodes == nil {
		observeData.Nodes = []foundation.UiNode{}
	}

	return &ObserveResult{
		RunDir:      runDir,
		RunID:       response.RunID,
		RequestID:   response.RequestID,
		PackageName: observeData.PackageName,
		Nodes:       observeData.Nodes,
		NodeCount:   len(observeData.Nodes),
	}, nil
}
