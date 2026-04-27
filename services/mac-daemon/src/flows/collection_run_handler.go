package flows

import (
	"encoding/json"
	"net/http"
	"os"
	"path/filepath"
	"time"

	"flowy/services/mac-daemon/src/state"
)

// CollectionRunRequest is the HTTP request body for POST /exp01/collection/run.
type CollectionRunRequest struct {
	DeviceID string            `json:"deviceId"`
	Config   CollectionConfig  `json:"config"`
	Profile  CollectionProfile `json:"profile"`
}

// CollectionRunHandler starts a collection flow and returns the result.
func CollectionRunHandler(app *state.AppState) http.HandlerFunc {
	return func(w http.ResponseWriter, r *http.Request) {
		var req CollectionRunRequest
		if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
			http.Error(w, "bad request: "+err.Error(), http.StatusBadRequest)
			return
		}
		if req.DeviceID == "" {
			http.Error(w, "deviceId is required", http.StatusBadRequest)
			return
		}

		session, ok := app.Client(req.DeviceID)
		if !ok {
			http.Error(w, "device not connected", http.StatusNotFound)
			return
		}

		// dedup store path: artifacts/<date>/<runId>/dedup.jsonl
		runDir := filepath.Join(app.ArtifactRoot(), time.Now().Format("2006-01-02"), "collection-"+time.Now().Format("150405"))
		_ = os.MkdirAll(runDir, 0755)
		dedupPath := filepath.Join(runDir, "dedup.jsonl")

		fc, err := NewFlowContext(session, app, app.ArtifactRoot(), req.Config, req.Profile, dedupPath)
		if err != nil {
			http.Error(w, "init flow: "+err.Error(), http.StatusInternalServerError)
			return
		}

		result := fc.Run()

		// Write result to artifact dir
		resultPath := filepath.Join(runDir, "collection-result.json")
		data, _ := json.MarshalIndent(result, "", "  ")
		_ = os.WriteFile(resultPath, data, 0644)

		w.Header().Set("Content-Type", "application/json")
		_ = json.NewEncoder(w).Encode(result)
	}
}
