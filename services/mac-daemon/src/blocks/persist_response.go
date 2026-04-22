package blocks

import (
	"os"
	"path/filepath"

	"flowy/services/mac-daemon/src/foundation"
)

func PersistResponse(runDir string, response any) error {
	data, err := foundation.EncodePretty(response)
	if err != nil {
		return err
	}
	if err := os.MkdirAll(runDir, 0o755); err != nil {
		return err
	}
	return os.WriteFile(filepath.Clean(foundation.ResponsePath(runDir)), data, 0o644)
}
