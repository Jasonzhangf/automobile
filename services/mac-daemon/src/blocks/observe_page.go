package blocks

import (
	"encoding/json"
	"fmt"
	"os"
	"path/filepath"
	"time"

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
	now := time.Now()
	requestID := foundation.NewRequestID(now, app.NextRequestSeq())
	runID := foundation.NewRunID(now, "dump-ui-tree-root", "observe-page")

	cmd := proto.CommandEnvelope{
		ProtocolVersion: "exp01",
		RequestID:       requestID,
		RunID:           runID,
		Command:         "dump-ui-tree-root",
		SentAt:          now.Format(time.RFC3339),
		TimeoutMs:       timeoutMs,
		Payload:         map[string]any{},
	}

	response, err := CommandRoundtrip(session, app, cmd)
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

	// Read the root-ui-tree artifact from the run directory.
	runDir := foundation.RunDirFromRunID(artifactRoot, response.RunID)
	treeData, err := os.ReadFile(filepath.Join(runDir, "root-ui-tree.json"))
	if err != nil {
		return nil, fmt.Errorf("failed to read root-ui-tree artifact: %w", err)
	}

	var treeDump foundation.UiTreeDump
	if err := json.Unmarshal(treeData, &treeDump); err != nil {
		return nil, fmt.Errorf("failed to parse root-ui-tree: %w", err)
	}

	return &ObserveResult{
		RunDir:      runDir,
		RunID:       response.RunID,
		RequestID:   response.RequestID,
		PackageName: treeDump.PackageName,
		Nodes:       treeDump.Nodes,
		NodeCount:   len(treeDump.Nodes),
	}, nil
}
