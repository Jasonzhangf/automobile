package flows

import (
	"slices"

	"flowy/services/mac-daemon/src/blocks"
	"flowy/services/mac-daemon/src/foundation"
	"flowy/services/mac-daemon/src/proto"
)

func FinalizeRun(artifactRoot string, response proto.ResponseEnvelope) (string, error) {
	runDir := foundation.RunDirFromRunID(artifactRoot, response.RunID)
	if err := blocks.PersistResponse(runDir, response); err != nil {
		return runDir, err
	}
	files := []string{"response.json"}
	if len(response.InlineLogs) > 0 {
		if err := blocks.PersistLogsArtifact(runDir, response.InlineLogs); err != nil {
			return runDir, err
		}
		files = append(files, "logs.txt")
	}
	for _, artifact := range response.Artifacts {
		if artifact.FileName == "" || slices.Contains(files, artifact.FileName) {
			continue
		}
		files = append(files, artifact.FileName)
	}
	manifest := proto.RunManifest{
		RunID:           response.RunID,
		RequestID:       response.RequestID,
		ProtocolVersion: response.ProtocolVersion,
		Command:         response.Command,
		CapturedAt:      response.FinishedAt,
		Status:          response.Status,
		Device:          response.Device,
		App:             response.App,
		Files:           files,
	}
	if err := blocks.PersistManifest(runDir, manifest); err != nil {
		return runDir, err
	}
	return runDir, nil
}
