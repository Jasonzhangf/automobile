package flows

import (
	"encoding/json"
	"errors"
	"net/http"

	"flowy/services/mac-daemon/src/blocks"
	"flowy/services/mac-daemon/src/foundation"
	"flowy/services/mac-daemon/src/proto"
	"flowy/services/mac-daemon/src/state"
)

func ArtifactUploadHandler(app *state.AppState) http.HandlerFunc {
	return func(writer http.ResponseWriter, request *http.Request) {
		meta, content, err := foundation.DecodeArtifactUpload(request)
		if err != nil {
			http.Error(writer, err.Error(), http.StatusBadRequest)
			return
		}
		if err := validateArtifactMeta(meta, content); err != nil {
			http.Error(writer, err.Error(), http.StatusBadRequest)
			return
		}
		runDir := foundation.RunDirFromRunID(app.ArtifactRoot(), meta.RunID)
		stored, err := blocks.PersistUploadedArtifact(runDir, meta, content)
		if err != nil {
			http.Error(writer, err.Error(), http.StatusInternalServerError)
			return
		}
		writer.Header().Set("Content-Type", "application/json")
		_ = json.NewEncoder(writer).Encode(proto.ArtifactUploadResponse{
			ProtocolVersion: meta.ProtocolVersion,
			RequestID:       meta.RequestID,
			RunID:           meta.RunID,
			Stored:          stored,
		})
	}
}

func validateArtifactMeta(meta proto.ArtifactUploadMeta, content []byte) error {
	if meta.ProtocolVersion != "exp01" {
		return errors.New("protocolVersion must be exp01")
	}
	if meta.RequestID == "" {
		return errors.New("requestId is required")
	}
	if meta.RunID == "" {
		return errors.New("runId is required")
	}
	if meta.DeviceID == "" {
		return errors.New("deviceId is required")
	}
	if meta.Kind == "" {
		return errors.New("kind is required")
	}
	if meta.FileName == "" {
		return errors.New("fileName is required")
	}
	if meta.ContentType == "" {
		return errors.New("contentType is required")
	}
	if len(content) == 0 {
		return errors.New("file content is empty")
	}
	return nil
}
