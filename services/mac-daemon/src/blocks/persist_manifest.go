package blocks

import (
	"os"
	"path/filepath"

	"flowy/services/mac-daemon/src/foundation"
)

func PersistManifest(runDir string, manifest any) error {
	data, err := foundation.EncodePretty(manifest)
	if err != nil {
		return err
	}
	if err := os.MkdirAll(runDir, 0o755); err != nil {
		return err
	}
	return os.WriteFile(filepath.Clean(foundation.ManifestPath(runDir)), data, 0o644)
}
