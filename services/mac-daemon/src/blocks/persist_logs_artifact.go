package blocks

import (
	"fmt"
	"os"
	"path/filepath"
	"strings"

	"flowy/services/mac-daemon/src/foundation"
	"flowy/services/mac-daemon/src/proto"
)

func PersistLogsArtifact(runDir string, entries []proto.LogEntry) error {
	if err := os.MkdirAll(runDir, 0o755); err != nil {
		return err
	}
	lines := make([]string, 0, len(entries))
	for _, entry := range entries {
		lines = append(lines, fmt.Sprintf("%s [%s] %s %s", entry.TS, entry.Level, entry.Event, entry.Message))
	}
	content := strings.Join(lines, "\n")
	if content != "" {
		content += "\n"
	}
	return os.WriteFile(filepath.Clean(foundation.LogsPath(runDir)), []byte(content), 0o644)
}
