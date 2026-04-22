package foundation

import (
	"path/filepath"
	"strings"
)

func RunDir(artifactRoot, capturedAt, runID string) string {
	return filepath.Join(artifactRoot, DatePartition(capturedAt), runID)
}

func RunDirFromRunID(artifactRoot, runID string) string {
	return filepath.Join(artifactRoot, DatePartitionFromRunID(runID), runID)
}

func DatePartitionFromRunID(runID string) string {
	if len(runID) < len("2006-01-02") {
		return "unknown-date"
	}
	datePart := runID[:len("2006-01-02")]
	if strings.Count(datePart, "-") != 2 {
		return "unknown-date"
	}
	return datePart
}

func ManifestPath(runDir string) string {
	return filepath.Join(runDir, "manifest.json")
}

func ResponsePath(runDir string) string {
	return filepath.Join(runDir, "response.json")
}

func LogsPath(runDir string) string {
	return filepath.Join(runDir, "logs.txt")
}

func ArtifactPath(runDir, fileName string) string {
	return filepath.Join(runDir, filepath.Base(fileName))
}
