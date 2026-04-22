package blocks

import (
	"errors"
	"os"
	"path/filepath"

	"flowy/services/mac-daemon/src/foundation"
	"flowy/services/mac-daemon/src/proto"
)

func PersistUploadedArtifact(runDir string, meta proto.ArtifactUploadMeta, content []byte) (proto.ArtifactDescriptor, error) {
	if meta.FileName == "" {
		return proto.ArtifactDescriptor{}, errors.New("missing fileName")
	}
	if meta.Kind == "" {
		return proto.ArtifactDescriptor{}, errors.New("missing kind")
	}
	if err := os.MkdirAll(runDir, 0o755); err != nil {
		return proto.ArtifactDescriptor{}, err
	}
	path := filepath.Clean(foundation.ArtifactPath(runDir, meta.FileName))
	if err := os.WriteFile(path, content, 0o644); err != nil {
		return proto.ArtifactDescriptor{}, err
	}
	return proto.ArtifactDescriptor{
		Kind:        meta.Kind,
		FileName:    filepath.Base(meta.FileName),
		ContentType: meta.ContentType,
		SHA256:      foundation.SHA256Hex(content),
		SizeBytes:   int64(len(content)),
	}, nil
}
